# 未完成重构的深度分析

## 概览

三个巨型类共 4127 行代码，占主要代码库的约 10%。

| 类名 | 行数 | 方法数 | 依赖文件数 | 复杂度 |
|------|------|--------|------------|--------|
| AircraftEntity | 1714 | 16个tick方法 | 未统计 | ⭐⭐⭐⭐⭐ |
| ShipCoreCombat | 1939 | 48个静态方法 | 未统计 | ⭐⭐⭐⭐⭐ |
| TransformationManager | 474 | 32个方法 | 24个文件 | ⭐⭐⭐⭐ |

---

## 重构方案 1：AircraftEntity 拆分（1714行）

### 当前问题

**职责过重**
- 状态机：5个状态 × 16个tick方法
- 战斗系统：7种攻击模式（战斗机/火箭/俯冲/水平/鱼雷/反潜/侦察）
- 侦察系统：区块加载 + 地图更新 + 网络同步
- 持久化：约100个字段的NBT序列化
- 客户端插值：防镜头抖动的位置同步
- 防御机制：stuck检测 + 距离限制 + 超时召回

**维护痛点**
- 修改一个攻击模式需要在1714行中定位
- 状态转换逻辑分散在多个方法中
- 新增飞机类型需要修改多处代码

**性能问题**
- 每个飞机实体每tick执行完整状态机
- 16个tick方法串行执行
- 侦察模式飞机也执行战斗逻辑判断

### 拆分方案 A：提取独立类（推荐）

```java
// 新文件：AircraftStateMachine.java
public class AircraftStateMachine {
    private final AircraftEntity entity;
    
    public void tick() {
        switch (entity.getState()) {
            case LAUNCHING -> tickLaunching();
            case CRUISING -> tickCruising();
            case ATTACKING -> tickAttacking();
            case RETURNING -> tickReturning();
            case RECON_ACTIVE -> tickReconActive();
        }
    }
}

// 新文件：AircraftCombat.java
public class AircraftCombat {
    public void executeAttack(AircraftEntity entity, LivingEntity target) {
        switch (entity.getPayloadType()) {
            case "fighter" -> fighterAttack(entity, target);
            case "rocket" -> rocketAttack(entity, target);
            // ... 其他攻击模式
        }
    }
}

// 新文件：AircraftRecon.java
public class AircraftRecon {
    public void tickRecon(AircraftEntity entity, Player owner) {
        updateChunkLoading(entity);
        updateMaps(entity, owner);
        syncToClient(entity, owner);
    }
}

// 简化后的 AircraftEntity.java (~800行)
public class AircraftEntity extends Entity {
    private final AircraftStateMachine stateMachine = new AircraftStateMachine(this);
    private final AircraftCombat combat = new AircraftCombat();
    private final AircraftRecon recon = new AircraftRecon();
    
    @Override
    public void tick() {
        stateMachine.tick();
    }
}
```

**优点**
- ✅ 职责清晰：状态机/战斗/侦察三个独立模块
- ✅ 易于测试：可以单独测试每个模块
- ✅ 易于扩展：新增攻击模式只修改 AircraftCombat
- ✅ 性能优化空间：侦察模式可以跳过战斗逻辑初始化
- ✅ 代码可读性：主类从1714行降至~800行

**缺点**
- ❌ 重构工作量大：需要重新组织100+字段的访问权限
- ❌ 测试成本高：需要验证所有7种攻击模式 × 5种状态
- ❌ 引入间接层：`entity.combat.executeAttack()` vs `entity.tickAttacking()`
- ❌ 字段访问复杂：需要大量getter/setter或package-private字段
- ❌ 破坏性变更风险：状态机逻辑复杂，容易引入bug

### 拆分方案 B：内部静态类（保守）

```java
public class AircraftEntity extends Entity {
    
    // 主类保留字段和基础方法
    
    // 内部类：状态机逻辑
    private static class StateMachine {
        static void tickLaunching(AircraftEntity e, Player owner) { ... }
        static void tickCruising(AircraftEntity e, Player owner) { ... }
        // ...
    }
    
    // 内部类：战斗策略
    private static class Combat {
        static void fighterAttack(AircraftEntity e, LivingEntity target) { ... }
        static void rocketAttack(AircraftEntity e, LivingEntity target) { ... }
        // ...
    }
    
    // 内部类：侦察系统
    private static class Recon {
        static void tickRecon(AircraftEntity e, Player owner) { ... }
        // ...
    }
    
    @Override
    public void tick() {
        switch (getState()) {
            case LAUNCHING -> StateMachine.tickLaunching(this, owner);
            case ATTACKING -> Combat.executeAttack(this, target);
            case RECON_ACTIVE -> Recon.tickRecon(this, owner);
            // ...
        }
    }
}
```

**优点**
- ✅ 保持单文件：不破坏现有文件结构
- ✅ 字段访问简单：内部类可以直接访问外部类字段
- ✅ 重构风险低：只是代码重新组织，不改变调用关系
- ✅ 工作量适中：2-3天可完成
- ✅ 逐步迁移：可以先迁移一部分方法测试

**缺点**
- ❌ 文件仍然很大：可能降至~1500行（改善有限）
- ❌ 测试隔离差：无法单独测试内部类
- ❌ IDE导航体验：内部类嵌套层级深

### 建议

**暂缓拆分，理由：**

1. **当前架构可维护**
   - 76行架构导航注释清晰标注了各模块入口
   - 方法命名规范（`tickXxx` / `resolveXxx`）
   - 已有的注释质量高

2. **拆分收益有限**
   - 主要问题是"文件大"，但逻辑清晰
   - 拆分后仍需维护状态机的复杂交互
   - 性能瓶颈不在代码组织，而在tick频率（已优化）

3. **风险大于收益**
   - 7种攻击模式 × 5种状态 = 35个测试场景
   - 状态转换逻辑复杂，容易引入bug
   - 100+字段的访问权限调整容易遗漏

**如果必须拆分，推荐方案B（内部静态类）**
- 风险可控，工作量适中
- 可以逐步迁移，先迁移侦察系统测试
- 保持单文件，降低维护成本

---

## 重构方案 2：TransformationManager 解耦（474行，24个依赖）

### 当前问题

**"上帝类"特征**
- 26个公共静态方法
- 职责混杂：变身管理 + 属性计算 + 负重系统 + 装备检测 + 冷却加速
- 24个文件依赖，成为核心耦合点

**具体职责分析**

| 职责 | 方法数 | 说明 |
|------|--------|------|
| 变身管理 | 5 | isTransformed / setTransformed / findTransformedCore |
| 属性计算 | 6 | applyTransformationAttributes / removeTransformationAttributes |
| 负重系统 | 8 | getItemLoad / getInventoryWeaponLoad / applyOverweightPenalty |
| 装备检测 | 4 | hasSonarEquipped / hasTorpedoReloadEquipped / isFireableWeapon |
| 冷却加速 | 3 | boostedCooldown / getReloadBoostLevel |

**维护痛点**
- 修改负重系统需要理解整个变身流程
- 新增装备类型需要修改多个方法
- 16个文件依赖导致修改影响面大

### 拆分方案：按职责拆分为独立类

```java
// 新文件：TransformationStateManager.java
public class TransformationStateManager {
    public static boolean isTransformed(ItemStack core) { ... }
    public static void setTransformed(ItemStack core, boolean state) { ... }
    public static ItemStack findTransformedCore(Player player) { ... }
}

// 新文件：ShipAttributeManager.java
public class ShipAttributeManager {
    public static void applyAttributes(Player player, ItemStack core) {
        double armor = calculateArmor(core);
        double speed = calculateSpeed(core);
        double health = calculateHealth(core);
        // 应用属性修饰符
    }
    
    public static void removeAttributes(Player player) { ... }
    
    private static double calculateArmor(ItemStack core) { ... }
    private static double calculateSpeed(ItemStack core) { ... }
}

// 新文件：LoadoutManager.java
public class LoadoutManager {
    private static final Map<Item, Integer> WEAPON_LOAD_MAP = new IdentityHashMap<>();
    
    public static int getItemLoad(ItemStack stack) { ... }
    public static int getInventoryWeaponLoad(Inventory inv) { ... }
    public static void applyOverweightPenalty(Player player, int load, int max) { ... }
}

// 新文件：EquipmentDetector.java
public class EquipmentDetector {
    public static boolean hasSonar(Player player, ItemStack core) { ... }
    public static boolean hasTorpedoReload(Player player, ItemStack core) { ... }
    public static boolean isFireableWeapon(ItemStack stack) { ... }
}

// 新文件：CooldownBooster.java
public class CooldownBooster {
    public static int boostedCooldown(Player player, int baseCooldown) {
        int level = getReloadBoostLevel(player);
        return (int) (baseCooldown / (1.0 + level * 0.2));
    }
}

// 保留 TransformationManager 作为门面（Facade）
public class TransformationManager {
    // 委托到各个子管理器
    public static boolean isTransformed(ItemStack core) {
        return TransformationStateManager.isTransformed(core);
    }
    
    public static void applyTransformationAttributes(Player player, ItemStack core) {
        ShipAttributeManager.applyAttributes(player, core);
    }
    
    // ... 其他委托方法
}
```

**优点**
- ✅ 职责单一：每个类只负责一个领域
- ✅ 易于测试：可以单独测试负重系统
- ✅ 易于扩展：新增装备类型只修改 EquipmentDetector
- ✅ 降低耦合：依赖方可以直接使用子管理器
- ✅ 代码可读性：每个类200行以内

**缺点**
- ❌ 重构工作量巨大：24个依赖文件需要逐一修改
- ❌ 测试成本极高：需要验证所有依赖方的行为不变
- ❌ 引入多层间接：`TransformationManager.applyAttributes()` → `ShipAttributeManager.applyAttributes()`
- ❌ 破坏性变更风险：属性计算逻辑复杂，容易遗漏边界情况
- ❌ 向后兼容问题：如果保留门面类，代码冗余；如果不保留，破坏性变更

### 建议

**强烈建议暂缓，理由：**

1. **高耦合是设计必然**
   - 变身系统是核心机制，24个依赖是合理的
   - 拆分后仍需维护各子系统间的协调逻辑
   - 门面模式会引入额外的间接层

2. **当前代码质量良好**
   - 474行对于核心管理器是可接受的
   - 方法命名清晰，职责虽多但边界明确
   - 静态方法便于全局调用

3. **拆分风险极高**
   - 24个依赖文件 × 平均3处调用 = 72个修改点
   - 属性计算涉及多个配置项，容易遗漏
   - 负重系统与属性系统有交互，拆分后需要跨类协调

4. **收益不明显**
   - 主要问题是"方法多"，但每个方法职责清晰
   - 拆分后测试复杂度不降反升（需要测试子系统间交互）
   - 性能无提升（静态方法调用开销极低）

**如果必须解耦，推荐渐进式方案：**
1. 先提取 LoadoutManager（负重系统相对独立）
2. 观察1-2个版本，验证稳定性
3. 再考虑提取其他模块

---

## 重构方案 3：ShipCoreCombat 拆分（1939行，48个方法）

### 当前问题

**职责过重**
- 火炮战斗：12种火炮 × 多种弹药类型
- 鱼雷战斗：水面鱼雷 + 潜艇鱼雷 + 鱼雷制导
- 航空战斗：7种飞机类型 × 自动发射逻辑
- 深弹战斗：反潜深弹投掷
- 导弹战斗：防空导弹 + 反舰导弹
- 弹药管理：装填 + 切换 + 容量检查
- 冷却管理：多槽位冷却 + 装填加速

**维护痛点**
- 新增武器类型需要在1939行中找到正确位置
- 战斗逻辑分散，难以统一优化
- 弹药系统与战斗系统耦合

### 拆分方案：按武器类型拆分

```java
// 新文件：CannonCombat.java
public class CannonCombat {
    public static boolean tryFireCannon(Level level, Player player, 
                                       ItemStack weapon, ItemStack core, int slot) {
        // 火炮发射逻辑
        // 弹道计算
        // 弹药消耗
        // 冷却设置
    }
    
    private static void fireSmallCannon(...) { ... }
    private static void fireMediumCannon(...) { ... }
    private static void fireLargeCannon(...) { ... }
    // ... 12种火炮
}

// 新文件：TorpedoCombat.java
public class TorpedoCombat {
    public static boolean tryFireTorpedo(Level level, Player player,
                                        ItemStack weapon, ItemStack core, int slot) {
        // 鱼雷发射逻辑
        // 制导系统
        // 水下检测
    }
}

// 新文件：AircraftCombat.java
public class AircraftCombat {
    public static boolean tryLaunchAircraft(Level level, Player player,
                                           ItemStack weapon, ItemStack core, int slot) {
        // 飞机发射逻辑
        // 燃料检查
        // 编队管理
    }
    
    public static void tryAutoLaunchFighter(...) { ... }
    public static void tryAutoFireAntiAirMissile(...) { ... }
}

// 新文件：AmmunitionManager.java
public class AmmunitionManager {
    public static boolean consumeAmmo(ItemStack core, int amount) { ... }
    public static void refillAmmo(ItemStack core) { ... }
    public static int getAmmoCount(ItemStack core) { ... }
}

// 新文件：CooldownManager.java
public class CooldownManager {
    public static void setCooldown(ItemStack core, int slot, int ticks) { ... }
    public static boolean isOnCooldown(ItemStack core, int slot) { ... }
    public static int getRemainingCooldown(ItemStack core, int slot) { ... }
}

// 保留 ShipCoreCombat 作为统一入口
public class ShipCoreCombat {
    public static boolean tryFireFromInventory(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        
        if (weapon.getItem() instanceof CannonItem) {
            return CannonCombat.tryFireCannon(level, player, weapon, core, slot);
        } else if (weapon.getItem() instanceof TorpedoItem) {
            return TorpedoCombat.tryFireTorpedo(level, player, weapon, core, slot);
        } else if (weapon.getItem() instanceof AircraftItem) {
            return AircraftCombat.tryLaunchAircraft(level, player, weapon, core, slot);
        }
        // ...
    }
}
```

**优点**
- ✅ 职责清晰：每种武器类型独立管理
- ✅ 易于扩展：新增火炮只修改 CannonCombat
- ✅ 易于测试：可以单独测试鱼雷系统
- ✅ 代码可读性：每个类300-500行
- ✅ 并行开发：不同开发者可以同时修改不同武器系统

**缺点**
- ❌ 重构工作量最大：1939行需要完全重组
- ❌ 测试成本最高：12种火炮 + 7种飞机 + 多种弹药 = 100+测试场景
- ❌ 共享逻辑提取困难：弹药/冷却/音效等逻辑需要抽象
- ❌ 破坏性变更风险最高：战斗系统是核心玩法，bug影响体验
- ❌ 性能回归风险：拆分后可能引入额外的方法调用开销

### 建议

**强烈建议暂缓，理由：**

1. **当前架构已经足够好**
   - 虽然1939行，但方法职责清晰
   - 按武器类型分组，注释详细
   - 静态方法便于全局调用

2. **拆分收益不明显**
   - 主要问题是"文件大"，但查找定位不困难
   - 拆分后仍需维护武器间的平衡性
   - IDE的"折叠方法"功能可以缓解阅读问题

3. **风险极高**
   - 战斗系统是核心玩法，bug会严重影响体验
   - 12种火炮 × 多种弹药 = 数十种组合需要测试
   - 弹道计算、伤害计算等精密逻辑容易出错

4. **工作量巨大**
   - 预计需要4-6天完成拆分
   - 需要2-3天进行全面测试
   - 可能需要1-2天修复回归bug

**如果必须拆分，推荐分阶段方案：**
1. 第一阶段：提取 AmmunitionManager 和 CooldownManager（相对独立）
2. 第二阶段：提取 AircraftCombat（飞机系统已有独立实体类）
3. 第三阶段：观察2-3个版本后，再考虑拆分火炮/鱼雷

---

## 总体建议

### 优先级排序

1. **不建议重构**：ShipCoreCombat（风险最高，收益最低）
2. **暂缓重构**：AircraftEntity（风险高，收益中等）
3. **可考虑重构**：TransformationManager（风险中等，但需渐进式）

### 何时应该重构？

**触发条件（满足任一即可考虑）：**

1. **维护痛点明确**
   - 连续3次修改同一类时遇到定位困难
   - 新增功能需要修改5个以上分散的方法
   - Bug修复后引入新bug的频率 > 20%

2. **性能瓶颈**
   - Profiler显示某个类的方法占用CPU > 10%
   - 玩家反馈明确的性能问题

3. **团队扩张**
   - 新成员加入，需要降低上手难度
   - 多人并行开发同一模块产生冲突

4. **架构演进需求**
   - 需要支持插件/扩展系统
   - 需要支持数据驱动配置

### 当前最佳实践

**不重构，而是改善现有代码：**

1. **增强注释**
   - 在类头部添加"快速导航"注释（AircraftEntity已有）
   - 标注各方法的调用关系和依赖
   - 添加性能注意事项

2. **提取常量**
   - 将魔法数字提取为命名常量
   - 将配置项集中管理

3. **增加单元测试**
   - 为关键方法添加测试（如弹道计算）
   - 使用测试覆盖率工具识别盲区

4. **使用IDE功能**
   - 方法折叠：隐藏不关心的方法
   - 书签：标记常用方法
   - 结构视图：快速跳转

5. **文档化**
   - 维护架构图（如状态机流程图）
   - 记录设计决策（为什么不拆分）
   - 编写维护指南

---

## 结论

**三个巨型类都不建议立即重构。**

**核心理由：**
- ✅ 当前代码质量良好（注释详细、逻辑清晰）
- ✅ 无明确的维护痛点（定位和修改仍然高效）
- ✅ 无性能瓶颈（已完成P0/P1优化）
- ❌ 重构风险极高（战斗系统是核心玩法）
- ❌ 测试成本巨大（数百个测试场景）
- ❌ 收益不明显（主要是"看起来大"，但不影响维护）

**推荐策略：**
1. 保持现状，持续改善注释和文档
2. 遇到具体痛点时，针对性地提取小模块
3. 积累2-3个版本的使用经验后，重新评估
4. 如果团队扩张或架构演进，再考虑大规模重构

**记住：过早优化是万恶之源，过度重构也是。**
