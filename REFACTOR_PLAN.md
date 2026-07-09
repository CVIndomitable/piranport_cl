# 皮兰港测试版架构重构计划

## 重构目标

将当前 61k 行/439 文件的单体架构重构为模块化、可测试、易维护的现代架构。

---

## 改进点 1: 模块化拆分 — 垂直切分玩法域

### 现状问题
- 单仓库 439 个 Java 文件，29 个顶层包，职责分散
- `combat`(29文件)、`aviation`(6文件)、`dungeon`(48文件) 三大玩法系统耦合在同一代码库
- 新增玩法时需要在多个顶层包之间穿插修改(entity/item/block/network/registry)

### 重构方案
创建 3 个子模块(Gradle subproject)，按玩法垂直切分：

```
piranport-core/          # 核心框架(注册表/事件总线/平台桥接/共享工具)
├── registry/            # 延迟注册表抽象(DeferredRegister包装)
├── event/               # 领域事件总线(EventBus + @Subscribe)
├── platform/            # ClientHooks 等平台桥接
├── util/                # 数学/碰撞/序列化等无状态工具
└── component/           # DataComponent 定义(所有子模块共享)

piranport-naval/         # 海战系统(变身/舰装/火炮/鱼雷/潜艇)
├── transformation/      # TransformationManager → TransformationService
├── weapon/              # 火炮/鱼雷发射器
├── projectile/          # 弹道求解/炮弹实体/鱼雷实体
├── equipment/           # 装甲板/声呐/引擎/鱼雷再装填
└── submarine/           # 潜艇特有逻辑

piranport-aviation/      # 航空系统(飞机/火控/侦察/反潜)
├── aircraft/            # 飞机实体/物品
├── firecontrol/         # 火控锁定/火控面板
├── recon/               # 侦察模式
└── asw/                 # 反潜声呐/深弹

piranport-dungeon/       # 副本系统(保持现有48文件结构，独立成模块)
├── instance/
├── lobby/
├── script/
├── event/
└── data/

piranport-content/       # 游戏内容注册(所有 Item/Block/Entity 注册)
├── items/               # ModItems.java 拆分为多个领域注册类
├── blocks/
├── entities/
└── creative/            # 创造模式标签页
```

### 依赖关系
- `piranport-content` 依赖 `core`、`naval`、`aviation`、`dungeon`(聚合注册)
- `naval`、`aviation`、`dungeon` 只依赖 `core`(通过事件总线解耦)
- `core` 不依赖任何子模块(纯框架层)

### 文件迁移示例
| 当前位置 | 新位置 |
|---------|--------|
| `combat/TransformationManager.java` | `naval/transformation/TransformationService.java` |
| `combat/BallisticSolver.java` | `naval/projectile/BallisticSolver.java` |
| `aviation/FireControlManager.java` | `aviation/firecontrol/FireControlService.java` |
| `dungeon/**` | `dungeon/**`(整体迁移) |
| `handler/PlayerTickHandler.java` | `core/event/PlayerTickEvent.java`(拆分为多个事件监听器) |

---

## 改进点 2: Handler/Helper 统一治理

### 现状问题
- 38 个 Handler/Helper 分散在 6 个包中(`handler/`、`client/`、`combat/`、`compat/`、`dungeon/`、根包)
- 职责混杂:事件监听、业务逻辑、无状态工具、状态管理器
- `ClientTickHandler.java` 在根包,违反架构测试的 rootPackageShouldBeClean 规则

### 重构方案

#### 2.1 事件监听器迁移到 `core/event/`
将 `@EventBusSubscriber` 类迁移到统一事件包，按生命周期命名：

```
core/event/
├── ModLifecycleEvents.java      # 原 CommonModEvents(Capability/属性注册)
├── ClientLifecycleEvents.java   # 原 ClientModEvents(渲染器/按键绑定)
├── PlayerTickEvents.java        # 拆分自 PlayerTickHandler
├── PlayerConnectionEvents.java  # 拆分自 PlayerConnectionHandler
├── ServerLifecycleEvents.java   # 原 ServerGameEvents
└── CombatEvents.java            # 伤害/击杀等战斗事件
```

#### 2.2 Manager 类规范化
所有 Manager 改名为 `*Service`，强制实现接口，移到子模块的 `service/` 包：

```java
// 原 combat/TransformationManager.java(静态方法类)
public final class TransformationManager {
    private TransformationManager() {}
    public static boolean isTransformed(ItemStack stack) { ... }
}

// 新 naval/transformation/TransformationService.java(服务接口 + 单例实现)
public interface TransformationService {
    boolean isTransformed(ItemStack stack);
    void applyAttributes(Player player, ItemStack core);
}

public final class TransformationServiceImpl implements TransformationService {
    private static final TransformationService INSTANCE = new TransformationServiceImpl();
    public static TransformationService getInstance() { return INSTANCE; }
    
    @Override
    public boolean isTransformed(ItemStack stack) { ... }
}

// 使用方从静态调用改为服务调用
TransformationService.getInstance().isTransformed(stack);
```

13 个 Manager 类全部迁移：
| 原类名 | 新位置 |
|--------|--------|
| `TransformationManager` | `naval/transformation/TransformationService` |
| `FireControlManager` | `aviation/firecontrol/FireControlService` |
| `ReconManager` | `aviation/recon/ReconService` |
| `TorpedoGuidanceManager` | `naval/projectile/TorpedoGuidanceService` |
| `SalvoManager` | `naval/weapon/SalvoService` |
| `DungeonInstanceManager` | `dungeon/instance/InstanceService` |
| `DungeonLobbyManager` | `dungeon/lobby/LobbyService` |
| `DungeonScriptManager` | `dungeon/script/ScriptService` |
| `FleetGroupManager` | `naval/npc/FleetService` |
| `SkinManager` | `naval/skin/SkinService` |
| `ScopingManager` | `naval/weapon/ScopeService` |
| `ConfigOverrideManager` | `content/config/ConfigService` |
| `FlagshipManager` | `dungeon/key/FlagshipService` |

#### 2.3 Helper 类规范化
无状态工具类迁移到 `core/util/` 或子模块的 `util/` 包：

```
core/util/
├── MathUtils.java              # 数学计算(三角/插值/随机)
├── CollisionUtils.java         # 碰撞检测/射线追踪
├── NBTUtils.java               # NBT 序列化工具
└── NetworkUtils.java           # 网络包工具方法

naval/util/
├── BallisticUtils.java         # 弹道计算辅助(从 BallisticSolver 抽取)
├── FuelUtils.java              # 燃料计算工具
└── LoadoutUtils.java           # 负重计算工具
```

有状态的 Helper 重命名为 `*Cache` 或合并到对应的 Service:
- `PlayerDataHelper` → `PlayerDataCache`(保留在 `core/cache/`)
- `PlayerAircraftHelper` → 合并到 `aviation/aircraft/AircraftService`
- `ReloadHelper` → 合并到 `naval/weapon/ReloadService`
- `FriendlyFireHelper` → 合并到 `naval/combat/DamageService`

---

## 改进点 3: 领域事件总线 — 解耦跨系统通信

### 现状问题
- 跨系统通信通过静态方法调用:`TransformationManager.applyTransformationAttributes(player, core)`
- 变身事件需要同步更新:属性、UI、音效、粒子效果 — 当前全部写在 `PlayerTickHandler` 的 600 行方法中
- 新增监听者需要修改原始调用方代码

### 重构方案

引入 Guava EventBus 风格的领域事件总线:

```java
// core/event/EventBus.java
public final class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();
    
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
    
    public <T> void post(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Consumer<?> consumer : list) {
                ((Consumer<T>) consumer).accept(event);
            }
        }
    }
}

// 领域事件定义
public record TransformationActivatedEvent(Player player, ItemStack core, ShipType type) {}
public record TransformationDeactivatedEvent(Player player, ItemStack core) {}
public record FuelDepletedEvent(Player player, ItemStack core) {}
public record WeaponFiredEvent(Player player, ItemStack weapon, Vec3 targetPos) {}
```

### 使用示例

```java
// 发布方(原 PlayerTickHandler.tickInventoryLoadCheck)
if (!TransformationManager.isTransformed(coreStack)) {
    TransformationManager.setTransformed(coreStack, true);
    TransformationManager.applyTransformationAttributes(player, coreStack);
    // 改为：
    EventBus.INSTANCE.post(new TransformationActivatedEvent(player, coreStack, type));
}

// 订阅方 1: 属性应用(naval/transformation/TransformationAttributeListener.java)
EventBus.INSTANCE.subscribe(TransformationActivatedEvent.class, event -> {
    applyAttributes(event.player(), event.core(), event.type());
});

// 订阅方 2: UI 提示(core/event/PlayerMessageListener.java)
EventBus.INSTANCE.subscribe(TransformationActivatedEvent.class, event -> {
    event.player().displayClientMessage(
        Component.translatable("message.piranport.transformed"), true);
});

// 订阅方 3: 粒子效果(naval/transformation/TransformationParticleListener.java)
EventBus.INSTANCE.subscribe(TransformationActivatedEvent.class, event -> {
    if (event.player().level() instanceof ServerLevel sl) {
        spawnTransformationParticles(sl, event.player());
    }
});
```

### 事件清单(初步规划)
```
TransformationActivatedEvent      # 变身激活
TransformationDeactivatedEvent    # 变身解除
FuelDepletedEvent                 # 燃料耗尽
WeaponFiredEvent                  # 武器发射
AircraftLaunchedEvent             # 飞机起飞
AircraftRecalledEvent             # 飞机召回
FireControlLockedEvent            # 火控锁定
DungeonNodeClearedEvent           # 副本节点通过
PlayerTeleportedEvent             # 玩家传送(燃料系统需要)
```

---

## 改进点 4: 测试覆盖 — 从 1 到 100+

### 现状问题
- 只有 1 个架构测试类(`ArchitectureTest.java`)
- 核心逻辑(伤害计算/弹道求解/负重系统)无单元测试
- 无集成测试,跨系统交互靠手工验证

### 重构方案

#### 4.1 单元测试覆盖核心逻辑
每个子模块建立 `src/test/java` 结构,测试覆盖率目标 70%+:

```
piranport-naval/src/test/java/
├── transformation/
│   ├── TransformationServiceTest.java        # 变身状态/属性计算
│   └── LoadCalculationTest.java              # 负重系统
├── projectile/
│   ├── BallisticSolverTest.java              # 弹道求解精度测试
│   └── TorpedoGuidanceTest.java              # 鱼雷制导逻辑
└── weapon/
    ├── SalvoSchedulingTest.java              # 齐射调度
    └── ReloadTimingTest.java                 # 装填加速计算

piranport-aviation/src/test/java/
├── firecontrol/
│   ├── TargetLockingTest.java                # 火控锁定/解锁
│   └── MultiTargetTest.java                  # 多目标管理
└── aircraft/
    ├── FuelConsumptionTest.java              # 飞机燃料消耗
    └── LaunchCooldownTest.java               # 起飞冷却

piranport-dungeon/src/test/java/
├── instance/
│   ├── InstanceLifecycleTest.java            # 副本生命周期
│   └── IndexRecyclingTest.java               # 索引回收(防泄漏)
└── script/
    ├── ScriptExecutionTest.java              # 脚本执行
    └── EventTriggerTest.java                 # 事件触发逻辑
```

#### 4.2 集成测试(Minecraft 测试环境)
使用 NeoForge 的测试框架 `GameTestHelper`:

```java
// naval/src/test/java/integration/TransformationIntegrationTest.java
@GameTest
public class TransformationIntegrationTest {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        return GameTestRegistry.register(
            "piranport.naval.transformation",
            "transformation_activates_with_fuel",
            test -> {
                ServerPlayer player = test.makeMockServerPlayer();
                ItemStack core = new ItemStack(ModItems.DD_CORE.get());
                core.set(ModDataComponents.SHIP_CORE_FUEL.get(), new FuelData(100, 100));
                
                player.getInventory().offhand.set(0, core);
                test.runAfterDelay(5, () -> {
                    test.assertTrue(TransformationService.getInstance().isTransformed(core),
                        "Player should transform with fuel");
                    test.succeed();
                });
            }
        );
    }
}
```

#### 4.3 性能测试(基准测试)
使用 JMH 测试热点路径性能:

```java
// core/src/jmh/java/event/EventBusBenchmark.java
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class EventBusBenchmark {
    @Benchmark
    public void postEventWith10Listeners(Blackhole bh) {
        EventBus bus = new EventBus();
        for (int i = 0; i < 10; i++) {
            bus.subscribe(TestEvent.class, e -> bh.consume(e));
        }
        bus.post(new TestEvent());
    }
}
```

---

## 改进点 5: 文档同步 — 从代码到知识库

### 现状问题
- 只有 `TransformationManager` 等少数类有详细注释
- 缺少包级别(`package-info.java`)文档说明职责边界
- 新人需要阅读代码才能理解架构

### 重构方案

#### 5.1 包级别文档
每个包添加 `package-info.java`,说明职责/依赖/使用场景:

```java
/**
 * 变身系统 — 玩家从人类形态到舰娘形态的切换管理。
 *
 * <h2>核心职责</h2>
 * <ul>
 *   <li>变身状态管理({@link TransformationService})</li>
 *   <li>属性计算(护甲/速度/血量/负重)</li>
 *   <li>超重惩罚(挖掘疲劳/虚弱/中毒)</li>
 *   <li>装填加速(RELOAD_BOOST 效果)</li>
 * </ul>
 *
 * <h2>依赖关系</h2>
 * <ul>
 *   <li>依赖: {@code core.event.EventBus}(发布变身事件)</li>
 *   <li>被依赖: {@code naval.weapon}(武器负重查询)</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 检查变身状态
 * boolean transformed = TransformationService.getInstance().isTransformed(coreStack);
 *
 * // 监听变身事件
 * EventBus.INSTANCE.subscribe(TransformationActivatedEvent.class, event -> {
 *     // 自定义逻辑
 * });
 * }</pre>
 *
 * @see TransformationService
 * @see TransformationActivatedEvent
 * @since 1.1.0
 */
package com.piranport.naval.transformation;
```

#### 5.2 架构决策记录(ADR)
在 `docs/adr/` 目录记录重要架构决策:

```
docs/adr/
├── 0001-modular-architecture.md          # 模块化拆分决策
├── 0002-event-bus-pattern.md             # 事件总线引入决策
├── 0003-service-interface-pattern.md     # Manager改Service决策
└── 0004-test-strategy.md                 # 测试策略决策
```

ADR 模板:
```markdown
# ADR-0001: 采用模块化架构

## 状态
已接受 (2026-07-09)

## 背景
单仓库 439 文件,新增玩法需要跨多个包修改,维护成本高。

## 决策
拆分为 5 个 Gradle 子模块(core/naval/aviation/dungeon/content)。

## 后果
- 正面:职责清晰,玩法独立开发,编译速度提升
- 负面:初期迁移成本高,需要调整构建脚本

## 实施时间表
- Phase 1: 创建子模块结构(1周)
- Phase 2: 迁移文件(2周)
- Phase 3: 测试&修复(1周)
```

#### 5.3 开发者文档
在 `docs/dev/` 目录添加系统文档:

```
docs/dev/
├── architecture.md               # 架构总览图(模块依赖/事件流)
├── transformation-system.md      # 变身系统详细设计
├── fire-control-system.md        # 火控系统详细设计
├── dungeon-system.md             # 副本系统详细设计
├── coding-standards.md           # 编码规范
└── testing-guide.md              # 测试指南
```

---

## 实施计划

### Phase 1: 基础设施搭建(1周)
1. 创建子模块 Gradle 配置
2. 搭建 `piranport-core` 模块:
   - 创建 `event/EventBus.java`
   - 迁移 `platform/ClientHooks.java`
   - 创建 `util/` 包结构
3. 编写 ADR 文档

### Phase 2: Naval 模块迁移(1周)
1. 迁移 `combat/` 包到 `naval/`:
   - `TransformationManager` → `transformation/TransformationService`
   - `BallisticSolver` → `projectile/BallisticSolver`
   - `SalvoManager` → `weapon/SalvoService`
2. 编写单元测试(目标覆盖率 70%)
3. 更新架构测试规则

### Phase 3: Aviation 模块迁移(3天)
1. 迁移 `aviation/` 包(6文件):
   - `FireControlManager` → `firecontrol/FireControlService`
   - `ReconManager` → `recon/ReconService`
2. 编写单元测试
3. 更新文档

### Phase 4: Dungeon 模块迁移(3天)
1. 整体迁移 `dungeon/` 包(48文件,保持子包结构)
2. 补充集成测试(副本生命周期/索引回收)
3. 更新文档

### Phase 5: Handler/Helper 重组(1周)
1. 事件监听器迁移到 `core/event/`
2. Helper 改造为 Service 或 Utils
3. 删除遗留的根包文件(`ClientTickHandler.java`)
4. 更新所有引用

### Phase 6: 事件总线接入(1周)
1. 定义领域事件类(10+ events)
2. 重构 `PlayerTickHandler` 为事件发布模式
3. 编写事件监听器
4. 性能测试(JMH)

### Phase 7: 测试完善(1周)
1. 补充集成测试(GameTest)
2. 补充性能测试(JMH)
3. 测试覆盖率达到 70%+

### Phase 8: 文档完善(3天)
1. 所有包添加 `package-info.java`
2. 编写系统文档(5篇)
3. 更新 CLAUDE.md

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 大规模重构导致功能回归 | 高 | Phase 2-4 每个模块迁移后立即跑完整测试套件 |
| 事件总线性能开销 | 中 | Phase 6 使用 JMH 基准测试,确保不劣于静态调用 |
| 子模块依赖管理复杂化 | 中 | 使用 Gradle 的 `api`/`implementation` 明确依赖范围 |
| 测试编写耗时超预期 | 低 | Phase 7 优先覆盖核心逻辑,边缘 case 延后 |

---

## 成功标准

- [ ] 代码结构:5 个子模块,每个模块职责单一
- [ ] 测试覆盖:单元测试 100+,集成测试 20+,覆盖率 70%+
- [ ] 文档完善:29 个包级别文档,5 篇系统文档,4 篇 ADR
- [ ] 架构测试:所有 ArchUnit 规则通过
- [ ] 性能无退化:事件总线开销 < 1% CPU 时间
- [ ] 构建速度:子模块独立编译速度提升 30%+

---

## 后续优化方向

完成本轮重构后,可考虑以下增强:
1. **依赖注入框架**:引入 Guice/Dagger 管理服务依赖
2. **数据驱动配置**:武器/飞机属性外部化到 JSON/YAML
3. **多语言支持**:i18n 文本抽取到资源文件
4. **CI/CD 流水线**:GitHub Actions 自动化测试/打包/发布
