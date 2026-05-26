# registry/items 子包说明

## 当前状态

本子包用于组织复杂物品的注册工厂方法，按类别拆分 ModItems.java 的注册逻辑。

### 已实现的工具类

- **BlockItems.java** - 方块物品注册（如烹饪锅、舰装核心修改器等）
- **FoodItems.java** - 食物物品注册（食材、半成品、成品菜肴、buff食物）
- **MaterialItems.java** - 材料物品注册（矿物、合金、零件等）

### 已删除的工具类（构造器签名不匹配）

以下类在 commit 06ec36f 中被删除，因为它们的构造器签名与 NeoForge 1.21.1 的 DeferredRegister.Items API 不匹配：

- **AircraftItems.java** - 飞机物品注册
- **WeaponItems.java** - 武器物品注册（火炮、鱼雷发射器、深弹发射器、导弹发射器）
- **DungeonItems.java** - 地牢相关物品注册
- **EquipmentItems.java** - 装备物品注册（装甲板、增强件等）
- **ShipCoreItems.java** - 舰装核心物品注册

## 修复指南

如需重新添加被删除的工具类，请参考现存的 `FoodItems.java` 和 `BlockItems.java` 的实现模式：

1. 所有工厂方法必须接受 `DeferredRegister.Items` 参数
2. 返回类型为 `DeferredItem<T extends Item>`
3. 使用 `registry.register(id, () -> new XxxItem(...))` 或 `registry.registerSimpleItem(id)` 注册
4. 复杂物品（如带 DataComponents 的物品）需要在 lambda 中构造

## 注意事项

- ModItems.java 保持不动（646 处外部引用兼容）
- 子包工具类仅作为可选的组织方式，不强制迁移现有注册代码
- 新增物品可以选择在 ModItems.java 直接注册，或在对应的子包工具类中添加工厂方法
