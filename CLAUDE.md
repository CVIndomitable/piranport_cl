# CLAUDE.md — 皮兰港 (Piran Port) Minecraft Mod

## Project Identity

- **Mod Name**: 皮兰港 / Piran Port
- **Mod ID**: `piranport`
- **Package**: `com.piranport`
- **License**: All Rights Reserved (同人项目)
- **Language**: Java 21
- **Platform**: NeoForge 21.11.x for Minecraft 1.21.11
- **Gradle Plugin**: ModDevGradle (推荐) 或 NeoGradle
- **MDK 模板**: https://github.com/NeoForgeMDKs/MDK-1.21.11-ModDevGradle

## What This Mod Is

基于战舰少女R世界观的 Minecraft 综合性模组。玩家可以扮演舰娘角色，装备舰载武器，与深海敌人战斗。

---

## Version Roadmap

| 版本 | 代号 | 状态 | 核心内容 |
|------|------|------|---------|
| v0.1.0-alpha | MVP | ✅ DONE | 基础注册、世界生成、合成、舰装核心GUI、火炮战斗 |
| v0.2.0-alpha | Torpedo | 🔨 CURRENT | 鱼雷系统、舰装栏GUI完善、负重平衡、装填机制 |
| v0.3.0-alpha | Kitchen | ⏳ PLANNED | 食物烹饪 + Buff系统 |
| v0.4.0-alpha | Deco | ⏳ PLANNED | 资源扩充、装饰方块、功能方块 |
| v0.5.0-alpha | Skin | ⏳ PLANNED | 皮肤/模型渲染系统 |

---

## ✅ v0.1.0-alpha — MVP (DONE)

<details>
<summary>已完成内容（折叠）</summary>

### Phase 1: 基础注册 (Items, Blocks, Creative Tab)
1. 矿物：矾土矿 (Bauxite Ore) → 铝锭 (Aluminum Ingot) → 铝块 (Aluminum Block)
2. 矿物：盐块 (Salt Block) → 盐 (Salt)
3. 舰装核心物品：小型/中型/大型 三种
4. 基础弹药物品：HE炮弹、AP炮弹，分小/中/大口径
5. 基础火炮物品：3种口径火炮
6. Creative Tab: `piranport_tab`

### Phase 2: 世界生成 (World Gen)
1. 矾土矿脉：高度 Y=-16 ~ 63，最大矿脉 9，替代 stone
2. 盐块：河流群系河床，替代 dirt

### Phase 3: 合成与冶炼 (Recipes)
1. 矾土 → 铝锭、铝锭 ↔ 铝块、盐块 ↔ 盐
2. 舰装核心、炮弹、火炮合成

### Phase 4: 舰装核心机制 (Data Components + GUI)
1. 舰装核心容器物品 + 右键 GUI
2. 武器栏 + 弹药库 + 负重显示

### Phase 5: 火炮战斗 (Entity + Combat)
1. CannonProjectileEntity 投射物
2. HE 爆炸 / AP 穿甲
3. 变身状态（Shift+右键）

</details>

---

## 🔨 v0.2.0-alpha — Torpedo (CURRENT)

**目标：加入鱼雷作为第二武器体系，同时完善舰装栏交互和装填机制。**

### Phase 6: 鱼雷物品与实体注册

**新增物品：**

| 注册 ID | 类 | 说明 |
|---------|-----|------|
| `torpedo_533mm` | `TorpedoItem` | 533mm 标准鱼雷弹药（可堆叠16） |
| `torpedo_610mm` | `TorpedoItem` | 610mm 氧气鱼雷弹药（可堆叠16） |
| `twin_torpedo_launcher` | `TorpedoLauncherItem` | 双联装鱼雷发射器（533mm，连装2） |
| `triple_torpedo_launcher` | `TorpedoLauncherItem` | 三联装鱼雷发射器（533mm，连装3） |
| `quad_torpedo_launcher` | `TorpedoLauncherItem` | 四联装鱼雷发射器（610mm，连装4） |

**新增实体：**

| 注册 ID | 类 | 说明 |
|---------|-----|------|
| `torpedo_entity` | `TorpedoEntity` | 鱼雷投射物（水面航行） |

**新增合成：**
- 鱼雷弹药：铁锭 + 火药 + 红石 + 铝锭 → torpedo
- 鱼雷发射器：铁锭 × N + 铝锭 + 红石 → launcher（N = 连装数相关）

**实现步骤：**
1. 创建 `TorpedoItem extends Item` — 弹药物品，带 `caliber` 属性（DataComponent 或构造参数）
2. 创建 `TorpedoLauncherItem extends Item` — 发射器物品，不可堆叠
3. 创建 `TorpedoEntity extends ThrowableProjectile` — 鱼雷实体
4. 在 `ModItems` 中注册所有新物品
5. 在 `ModEntityTypes` 中注册 `TORPEDO_ENTITY`
6. DataGen：物品模型、语言文件、合成表
7. 验证：Creative Tab 中能拿到所有鱼雷相关物品

### Phase 7: 鱼雷物理与 AI

鱼雷不是抛物线飞行的炮弹，而是**水面航行的自驱动实体**。

**TorpedoEntity 核心 tick 逻辑（每 tick 执行）：**

```
1. 剩余航程检查：lifetime-- ≤ 0 → discard()
2. 碰撞检查：命中实体 → onHitEntity()；命中方块 → onHitBlock()
3. 水面贴合：
   a. 当前位置是空气 && 下方 0.2 格是水 → 垂直速度设 0，水平航行
   b. 当前位置在水中 → 向上加速 (vy += 0.04)
   c. 都不满足 → 向下加速 (vy -= 0.06，受重力下坠)
4. 水平运动：保持发射时的水平方向匀速 (不受水平阻力)
```

**关键参数：**

| 参数 | 533mm | 610mm |
|------|-------|-------|
| 伤害 | 18 | 28 |
| 航速 (blocks/tick) | 1.2 | 1.0 |
| 航程 (ticks) | 200 (10s) | 300 (15s) |
| 爆炸范围 | 2.0 | 2.5 |

**伤害模式：**
- `onHitEntity`: 造成固定伤害，附加 3s 进水 debuff（自定义 MobEffect，每秒 1 点魔法伤害）
- `onHitBlock`: 水中不爆炸不破坏方块；空气中命中方块则小范围爆炸

**实现步骤：**
1. 在 `TorpedoEntity#tick()` 中实现水面贴合 AI
2. 实现 `onHitEntity` / `onHitBlock` 伤害逻辑
3. 注册 `ModMobEffects` — 进水 (Flooding) MobEffect
4. 鱼雷实体渲染器（先用简单盒子模型，后续美化）
5. 验证：站在水边发射鱼雷，鱼雷在水面滑行，命中生物造成伤害

### Phase 8: 鱼雷发射器机制

**TorpedoLauncherItem 使用逻辑：**

```
玩家按使用键 →
  检查是否变身状态 → 否：提示文字，return
  检查舰装栏弹药库是否有对应口径鱼雷 → 否：提示，return
  生成「连装数」枚 TorpedoEntity，扇形散布 →
    - 2 联装：±3° 偏角
    - 3 联装：-4°, 0°, +4°
    - 4 联装：-6°, -2°, +2°, +6°
  消耗弹药库中「连装数」枚鱼雷物品
  消耗发射器 1 点耐久
  进入冷却（不同于火炮自动装填，鱼雷发射器有固定 cooldown）
```

**发射器参数：**

| 发射器 | 口径 | 连装数 | 冷却(tick) | 耐久 | 负重 |
|--------|------|--------|-----------|------|------|
| 双联装 | 533mm | 2 | 100 (5s) | 64 | 8 |
| 三联装 | 533mm | 3 | 100 (5s) | 48 | 12 |
| 四联装 | 610mm | 4 | 120 (6s) | 32 | 20 |

**实现步骤：**
1. `TorpedoLauncherItem#use()` — 发射逻辑 + 扇形散布
2. 弹药消耗：从 ShipCoreContents 的 ammo 列表中按类型匹配消耗
3. 冷却：使用 `player.getCooldowns().addCooldown(item, ticks)`
4. 耐久：`stack.hurtAndBreak(1, player, ...)`
5. 验证：变身后使用鱼雷发射器，扇形发射多枚鱼雷

### Phase 9: 舰装栏 GUI 完善

**在 v0.1.0 基础上增加以下功能：**

#### 9a. 强化栏 (Enhancement Slots)

在 ShipCoreMenu / ShipCoreScreen 中新增 **强化栏**（2-4 slots，取决于核心类型）。

强化栏目前只放一种物品：**附加装甲**（Phase 9 同时注册）。

| 注册 ID | 说明 | 护甲值 | 负重 |
|---------|------|--------|------|
| `small_armor_plate` | 小型附加装甲 | +2 | 10 |
| `medium_armor_plate` | 中型附加装甲 | +4 | 20 |
| `large_armor_plate` | 大型附加装甲 | +6 | 30 |

变身后，强化栏内的装甲自动生效（通过 `AttributeModifier` 加在玩家 `Attributes.ARMOR` 上）。

#### 9b. 变身时武器栏/强化栏锁定

策划案要求变身后武器栏和强化栏锁定，防止偷换负重。实现方式：

- `ShipCoreMenu` 中增加 `locked` 状态字段
- 当 `TransformationManager.isTransformed(player)` 为 true 时，对应 Slot 的 `mayPlace()` / `mayPickup()` 返回 false
- GUI 中锁定 Slot 渲染灰色遮罩
- 弹药库 Slot 始终不锁定

#### 9c. 负重 → 移速影响

变身后，根据当前负重百分比影响移速：

```
speedMultiplier = 1.0 - (currentLoad / maxLoad) * 0.6
最低不低于 0.4（即满负重时仍有 40% 移速）
```

通过 `AttributeModifier` 加在 `Attributes.MOVEMENT_SPEED` 上，每次打开/关闭 GUI 时重新计算。

#### 9d. 装填进度 HUD

变身状态下，武器栏中每把武器的装填进度显示在 HUD 上：

- 使用 `RenderGuiLayerEvent.Post` 在快捷栏上方绘制
- 每个武器 Slot 下方显示一条细进度条
- 火炮：自动装填倒计时
- 鱼雷：用 `player.getCooldowns().getCooldownPercent()` 显示冷却

**实现步骤：**
1. 扩展 `ShipCoreContents` record：增加 `List<ItemStack> enhancements` 字段
2. 更新 Codec / StreamCodec（enhancements 用 `optionalFieldOf` 向后兼容）
3. 扩展 `ShipCoreMenu`：增加强化栏 Slot
4. 更新 `ShipCoreScreen` 绘制：强化栏位 + 锁定遮罩
5. 注册附加装甲物品（`ArmorPlateItem`）
6. `TransformationManager` 增加：装甲属性加成、移速计算
7. 新增 `ShipCoreHudLayer` 客户端类，绘制装填进度条
8. 验证：变身后 GUI 锁定正确，装甲加护甲，满负重明显减速，HUD 可见装填进度

### Phase 10: 数值平衡验证

在所有功能实现后，进行一轮手动平衡测试：

**测试场景：**
1. 小型核心 + 2把小炮 + 双联鱼雷 → 总负重应约 20/40（50%），移速中等
2. 大型核心 + 2把大炮 + 四联鱼雷 + 大装甲 → 总负重约 100/112（89%），移速很慢但火力强
3. 中型核心尝试塞满 → 应该存在"放不下"的情况，体现取舍

**平衡检查清单：**
- [ ] 小型核心不能装大型火炮（负重超限时 GUI 拒绝放入或红色警告）
- [ ] 鱼雷 DPS 应略高于同负重火炮（因为弹药有限、需贴近使用）
- [ ] 满负重移速不至于完全无法移动
- [ ] HE 炮弹 vs AP 炮弹有明显的使用场景区分

**不需要写代码，只需要调参数。** 如果发现严重失衡，修改本文档中的数值表并更新代码中的常量。

---

## NOT In v0.2.0 (明确排除)

- ❌ 声导/磁性鱼雷（追踪 AI）— 留到后续版本
- ❌ 潜射鱼雷 / 潜艇核心 — 留到后续版本
- ❌ 鱼雷再装填功能方块 — MVP 直接消耗弹药库存
- ❌ 航空、导弹、反潜系统
- ❌ 皮肤/模型渲染
- ❌ NPC、副本、食物

---

## Project Structure (v0.2.0 更新)

```
src/main/java/com/piranport/
├── PiranPort.java
├── registry/
│   ├── ModItems.java               # + 鱼雷、发射器、装甲
│   ├── ModBlocks.java
│   ├── ModCreativeTabs.java
│   ├── ModEntityTypes.java         # + TORPEDO_ENTITY
│   ├── ModDataComponents.java      # ShipCoreContents 扩展
│   └── ModMobEffects.java          # 🆕 进水 (FLOODING) effect
├── item/
│   ├── ShipCoreItem.java
│   ├── CannonItem.java
│   ├── ShellItem.java
│   ├── TorpedoItem.java            # 🆕 鱼雷弹药
│   ├── TorpedoLauncherItem.java    # 🆕 鱼雷发射器
│   └── ArmorPlateItem.java         # 🆕 附加装甲
├── block/
│   ├── BauxiteOreBlock.java
│   └── SaltBlock.java
├── entity/
│   ├── CannonProjectileEntity.java
│   └── TorpedoEntity.java          # 🆕 鱼雷实体
├── effect/
│   └── FloodingEffect.java         # 🆕 进水 MobEffect
├── menu/
│   ├── ShipCoreMenu.java           # 扩展：强化栏 + 锁定逻辑
│   └── ShipCoreScreen.java         # 扩展：强化栏绘制 + 锁定遮罩
├── client/
│   ├── ShipCoreHudLayer.java       # 🆕 装填进度 HUD
│   └── TorpedoRenderer.java        # 🆕 鱼雷实体渲染
├── worldgen/
│   └── ModOrePlacement.java
├── combat/
│   └── TransformationManager.java  # 扩展：装甲加成、移速计算
├── data/
│   ├── ModBlockStateProvider.java
│   ├── ModItemModelProvider.java   # + 新物品模型
│   ├── ModRecipeProvider.java      # + 新合成表
│   ├── ModLootTableProvider.java
│   └── ModWorldGenProvider.java
└── network/
    └── ModPackets.java
```

---

## Technical Reference

### NeoForge 1.21.11 核心 API 用法

#### 注册系统 — 使用 DeferredRegister

```java
// ModItems.java
public class ModItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(PiranPort.MOD_ID);

    public static final DeferredItem<Item> ALUMINUM_INGOT =
        ITEMS.registerSimpleItem("aluminum_ingot");

    public static final DeferredItem<Item> SALT =
        ITEMS.registerSimpleItem("salt");

    public static final DeferredItem<ShipCoreItem> SMALL_SHIP_CORE =
        ITEMS.register("small_ship_core",
            () -> new ShipCoreItem(new Item.Properties().stacksTo(1),
                ShipCoreItem.ShipType.SMALL));

    // v0.2.0 新增
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM =
        ITEMS.register("torpedo_533mm",
            () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533));

    public static final DeferredItem<TorpedoLauncherItem> TWIN_TORPEDO_LAUNCHER =
        ITEMS.register("twin_torpedo_launcher",
            () -> new TorpedoLauncherItem(
                new Item.Properties().stacksTo(1).durability(64),
                533, 2, 100));  // caliber, tubeCount, cooldownTicks
}
```

#### 主类结构

```java
@Mod(PiranPort.MOD_ID)
public class PiranPort {
    public static final String MOD_ID = "piranport";

    public PiranPort(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);  // v0.2.0

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 非注册类初始化
    }
}
```

#### 鱼雷实体核心实现

```java
public class TorpedoEntity extends ThrowableProjectile {
    private int lifetime;       // 剩余航程 ticks
    private float torpedoSpeed; // blocks/tick
    private float damage;

    @Override
    public void tick() {
        super.tick();
        if (--lifetime <= 0) { discard(); return; }

        // 水面贴合 AI
        BlockPos pos = blockPosition();
        BlockPos below = pos.below();
        boolean inAir = level().getBlockState(pos).isAir();
        boolean waterBelow = level().getBlockState(below).getFluidState().is(Fluids.WATER);
        boolean inWater = level().getBlockState(pos).getFluidState().is(Fluids.WATER);

        Vec3 motion = getDeltaMovement();
        if (inAir && waterBelow) {
            // 水面航行：清除垂直速度
            setDeltaMovement(motion.x, 0, motion.z);
        } else if (inWater) {
            // 在水中：上浮
            setDeltaMovement(motion.x, motion.y + 0.04, motion.z);
        } else {
            // 空中：下坠
            setDeltaMovement(motion.x, motion.y - 0.06, motion.z);
        }

        // 水平运动由初始发射方向决定，不额外处理
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide()) {
            result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
            // 附加进水 debuff
            if (result.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(
                    ModMobEffects.FLOODING, 60, 0));  // 3s
            }
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide()) {
            // 水中命中方块：静默消失
            if (level().getBlockState(result.getBlockPos()).getFluidState().is(Fluids.WATER)) {
                discard();
                return;
            }
            // 空气中命中方块：爆炸
            level().explode(this, getX(), getY(), getZ(),
                2.0f, Level.ExplosionInteraction.TNT);
            discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0; // 重力由自定义 tick 逻辑控制
    }
}
```

#### 鱼雷发射器扇形散布

```java
public class TorpedoLauncherItem extends Item {
    private final int caliber;
    private final int tubeCount;
    private final int cooldownTicks;

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!TransformationManager.isTransformed(player)) {
            // 提示需要变身
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }

        ItemStack stack = player.getItemInHand(hand);

        // 从舰装核心弹药库消耗鱼雷
        int consumed = consumeTorpedoAmmo(player, caliber, tubeCount);
        if (consumed == 0) {
            // 提示弹药不足
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide()) {
            // 扇形发射
            float[] angles = getSpreadAngles(consumed);
            Vec3 look = player.getLookAngle();
            for (float angleOffset : angles) {
                Vec3 dir = rotateY(look, Math.toRadians(angleOffset));
                TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
                torpedo.setPos(player.getX() + dir.x * 0.5,
                               player.getEyeY() - 0.3,
                               player.getZ() + dir.z * 0.5);
                torpedo.setDeltaMovement(dir.normalize().scale(torpedo.getTorpedoSpeed()));
                level.addFreshEntity(torpedo);
            }
        }

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(this, cooldownTicks);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private float[] getSpreadAngles(int count) {
        return switch (count) {
            case 2 -> new float[]{-3f, 3f};
            case 3 -> new float[]{-4f, 0f, 4f};
            case 4 -> new float[]{-6f, -2f, 2f, 6f};
            default -> new float[]{0f};
        };
    }
}
```

#### 进水 MobEffect

```java
public class FloodingEffect extends MobEffect {
    public FloodingEffect() {
        super(MobEffectCategory.HARMFUL, 0x3366AA); // 蓝色
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 每秒 1 点魔法伤害（20 tick 调用一次）
        entity.hurt(entity.damageSources().magic(), 1.0f);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0; // 每 20 tick (1秒) 触发一次
    }
}
```

#### 装填进度 HUD (Client)

```java
@Mod.EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ShipCoreHudLayer {
    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        // 仅 HOTBAR 层之后绘制
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !TransformationManager.isTransformed(player)) return;

        GuiGraphics gui = event.getGuiGraphics();
        // 读取舰装核心中各武器 Slot 的装填/冷却进度
        // 在快捷栏上方绘制进度条（每 Slot 宽 18px，高 2px）
        // 火炮：用 reloadProgress / maxReloadTime
        // 鱼雷：用 player.getCooldowns().getCooldownPercent(item, partialTick)
    }
}
```

#### ShipCoreContents 扩展 (v0.2.0)

```java
public record ShipCoreContents(
    List<ItemStack> weapons,
    List<ItemStack> ammo,
    List<ItemStack> enhancements,  // 🆕 强化栏
    int currentLoad
) {
    public static final Codec<ShipCoreContents> CODEC = RecordCodecBuilder.create(i -> i.group(
        ItemStack.CODEC.listOf().fieldOf("weapons").forGetter(ShipCoreContents::weapons),
        ItemStack.CODEC.listOf().fieldOf("ammo").forGetter(ShipCoreContents::ammo),
        ItemStack.CODEC.listOf().optionalFieldOf("enhancements", List.of())
            .forGetter(ShipCoreContents::enhancements),
        Codec.INT.fieldOf("current_load").forGetter(ShipCoreContents::currentLoad)
    ).apply(i, ShipCoreContents::new));
}
```

> ⚠️ **向后兼容**：旧版 ShipCoreContents 没有 `enhancements` 字段。
> Codec 中用 `.optionalFieldOf("enhancements", List.of())` 处理缺失情况。

---

## Key Design Decisions

### 负重系统数值表

| 核心类型 | 血量加成 | 负重上限 | 武器栏数 | 强化栏数 | 弹药库 |
|---------|---------|---------|---------|---------|-------|
| 小型    | +0      | 40      | 4       | 2       | 4     |
| 中型    | +5      | 64      | 5       | 3       | 4     |
| 大型    | +10     | 112     | 6       | 4       | 4     |

### 火炮数值表

| 火炮    | 口径     | 伤害  | 装填(tick) | 负重 |
|--------|---------|------|-----------|------|
| 小型火炮 | 3-6 in  | 6    | 30        | 6    |
| 中型火炮 | 7-13 in | 12   | 50        | 16   |
| 大型火炮 | 14-21in | 20   | 80        | 30   |

### 炮弹数值表

| 炮弹 | 效果 | 特性 |
|------|------|------|
| HE   | 爆炸伤害 | 命中时小范围爆炸 (power=1.5)，可伤害多目标 |
| AP   | 穿甲伤害 | 直击伤害 130% of HE，忽略部分护甲，无 AOE |

### 鱼雷数值表 (v0.2.0)

| 弹药 | 伤害 | 航速(b/t) | 航程(s) | 爆炸范围 |
|------|------|----------|---------|---------|
| 533mm 标准鱼雷 | 18 | 1.2 | 10 | 2.0 |
| 610mm 氧气鱼雷 | 28 | 1.0 | 15 | 2.5 |

### 鱼雷发射器数值表 (v0.2.0)

| 发射器 | 口径 | 连装数 | 冷却(s) | 耐久 | 负重 |
|--------|------|--------|---------|------|------|
| 双联装 533mm | 533 | 2 | 5 | 64 | 8 |
| 三联装 533mm | 533 | 3 | 5 | 48 | 12 |
| 四联装 610mm | 610 | 4 | 6 | 32 | 20 |

### 装甲数值表 (v0.2.0)

| 装甲 | 护甲加成 | 负重 |
|------|---------|------|
| 小型附加装甲 | +2 | 10 |
| 中型附加装甲 | +4 | 20 |
| 大型附加装甲 | +6 | 30 |

### DPS 对比参考 (平衡目标)

| 配装 | 单发伤害 | 周期(s) | DPS | 负重 | 备注 |
|------|---------|---------|-----|------|------|
| 小炮 ×2 | 6 ×2 | 1.5 | 8.0 | 12 | 远程稳定输出 |
| 中炮 ×1 | 12 | 2.5 | 4.8 | 16 | 单炮中远程 |
| 双联鱼雷 ×1 | 18 ×2 | 5.0 | 7.2 | 8 | 高爆发+debuff，需贴近 |
| 四联鱼雷 ×1 | 28 ×4 | 6.0 | 18.7 | 20 | 极高爆发，弹药有限 |

> 鱼雷 DPS 纸面偏高，但受限于：弹药有限（不自动装填）、需在水面使用、需贴近。

---

## Conventions & Rules

### 命名规范
- 类名: `PascalCase` — `ShipCoreItem`, `TorpedoEntity`
- 注册 ID: `snake_case` — `small_ship_core`, `torpedo_533mm`
- 常量: `UPPER_SNAKE_CASE` — `SMALL_SHIP_CORE`, `TORPEDO_533MM`
- 包名: 全小写 — `com.piranport.registry`
- 翻译 key: `item.piranport.torpedo_533mm`, `effect.piranport.flooding`

### 代码规范
- 所有注册走 `DeferredRegister`，禁止直接 `Registry.register()`
- 所有物品数据存储用 `DataComponents`，不用旧版 NBT 的 `CompoundTag`
- Client-only 代码放在 `@Mod.EventBusSubscriber(value = Dist.CLIENT)` 中
- 所有硬编码数值提取为常量或 config
- 每个系统一个包，不要把所有东西塞在一个类里
- 新增实体必须同时注册渲染器（`EntityRenderersEvent.RegisterRenderers`）

### 资源文件规范
- 贴图：16x16 PNG，风格统一（像素风，配色参考战舰少女R）
- 模型 JSON：先用基础 `item/generated` 和 `block/cube_all`
- 语言文件：同时维护 `zh_cn.json` 和 `en_us.json`
- 用 DataGen 生成所有能生成的 JSON

### 测试规范
- 每完成一个 Phase 必须能 `gradlew runClient` 启动并验证
- 鱼雷：站在水边发射，确认水面贴合航行、命中伤害、进水debuff
- GUI：变身后打开确认武器栏/强化栏锁定，弹药库可操作
- 负重：满负重确认移速下降，超负重确认拒绝放入
- HUD：变身后确认装填/冷却进度条显示

---

## Build & Run

```bash
# 运行客户端测试
./gradlew runClient

# 运行 DataGen
./gradlew runData

# 构建发布 JAR
./gradlew build
# 输出: build/libs/piranport-0.2.0-alpha.jar
```

### gradle.properties 关键配置

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=0.2.0-alpha
mod_group_id=com.piranport
mod_authors=PiranPort Dev Team
mod_description=Minecraft mod based on Warship Girls R
minecraft_version=1.21.11
neo_version=21.11.0-beta
```

---

## Phase 实施顺序 (v0.2.0)

严格按以下顺序，**每个 Phase 完成后必须可运行验证**：

1. **Phase 6** → Creative Tab 出现鱼雷弹药和发射器，物品可拿取
2. **Phase 7** → 鱼雷实体在水面航行，命中造成伤害+进水debuff
3. **Phase 8** → 变身后使用发射器扇形发射鱼雷，消耗弹药和耐久
4. **Phase 9** → GUI 显示强化栏，变身锁定，装甲加成，移速影响，HUD 装填进度
5. **Phase 10** → 数值检查通过，无严重失衡

**不要跳步。不要提前做后续 Phase 的内容。**

---

## Reference Links

- NeoForge Docs: https://docs.neoforged.net/
- NeoForge 21.11 Release Notes: https://neoforged.net/news/21.11release/
- MDK Template: https://github.com/NeoForgeMDKs/MDK-1.21.11-ModDevGradle
- Minecraft Wiki (Technical): https://minecraft.wiki/
- 原始策划案：见项目根目录 `docs/总策划案.docx`
