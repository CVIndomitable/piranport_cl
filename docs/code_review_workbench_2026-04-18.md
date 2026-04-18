# 合成台系统代码审查与修复（2026-04-18）

## 范围

武器合成台 + 弹药合成台（11 个文件）：
- `crafting/WeaponWorkbenchRecipe.java`
- `crafting/WeaponWorkbenchRecipeRegistry.java`
- `menu/WeaponWorkbenchMenu.java`
- `menu/WeaponWorkbenchScreen.java`
- `menu/AmmoWorkbenchMenu.java`
- `menu/AmmoWorkbenchScreen.java`
- `block/WeaponWorkbenchBlock.java`
- `block/AmmoWorkbenchBlock.java`
- `block/entity/WeaponWorkbenchBlockEntity.java`
- `block/entity/AmmoWorkbenchBlockEntity.java`
- `network/AmmoWorkbenchCraftPayload.java`

## 修复统计

| 级别 | 数量 |
|------|------|
| P0   | 4    |
| P1   | 10   |
| P2   | 16（实施 13，设计保留 3）|
| P3   | 14（实施 10，设计保留 4）|
| 合计 | 44（实施 37）|

## 架构级变更

### 弹药合成台扣料流程重构
改造前：`AmmoWorkbenchCraftPayload` 在服务端直接从玩家背包 `shrink` 材料，再调 `be.startCrafting`。BE 的 serverTick 独立推进，存在：
- 材料已扣但产物槽满时卡死 → 玩家材料蒸发或刷物
- 扣料后玩家可立即关 GUI/离线
- 其他玩家可抢走产物
- recipe 跨版本删除时材料吞失

改造后：
- 新增 `pendingMaterials`（NonNullList<ItemStack>）缓冲，已从玩家背包取出的原料暂存于 BE，持久化
- 新增 `pendingOutput` ItemStack，产物槽满时暂存，serverTick 尝试 flush
- 新增 `craftingOwner`（UUID），绑定发起合成的玩家；产物仅该玩家可 pickup；完成并取走后才清空
- `cancelCrafting()` 把 pendingMaterials 归还 owner 或 drop 到地面
- `recipe == null` 时走 `refundPendingMaterials` 补偿材料
- `dumpContentsOnBreak()` 在方块破坏时一次性 drop 所有槽、pending 物品

### ItemHandler 自动化保护
两个 BE 的 `ItemStackHandler` 覆盖：
- `isItemValid` — OUTPUT_SLOT 一律拒绝外部插入
- `insertItem` — OUTPUT 拒绝；合成中所有槽拒绝（Weapon）
- `extractItem` — 合成中非输出槽拒绝（Weapon）

AmmoWorkbench 的输出槽唯一且不接受自动化插入。`Block.onRemove` 会先 `cancelCrafting` 解锁 handler 再执行 extract，避免合成中方块被破坏时物品残留。

### 网络包防护
`AmmoWorkbenchCraftPayload`：
- `readUtf` 限长 64 字节
- 每玩家每 10 tick 最多 1 次请求（WeakHashMap 节流）
- pos Y 范围校验（Level min/max build height）
- 必须 `player.containerMenu instanceof AmmoWorkbenchMenu` 且 pos 一致
- `stillValid` 二次校验

## 保留未修条目

| # | 级别 | 条目 | 保留原因 |
|---|------|------|----------|
| #21 | P2 | AmmoWorkbench 多玩家无互斥 | 设计：允许任何玩家查看进度，产物已通过 craftingOwner 绑定 |
| #23 | P2 | getResultStack 无缓存 | ItemStack 可变，跨帧缓存会共享泄露，风险大于收益 |
| #30 | P2 | 硬编码 Item 引用缺 Tag 支持 | 需要 Recipe schema 整体重构 |
| #31 | P3 | 两套架构未抽象 | 业务语义差异大（武器按原料槽合成/弹药按数量合成），强行抽象耦合更重 |
| #33 | P3 | Screen 布局魔法坐标 | UI 布局稳定无需重构 |
| #36 | P3 | canCraftClient 与 BE.canCraft 重复 | 客户端为渲染提示，服务端为授权，分层合理 |
| #43 | P3 | ESC 在输入框吞事件 | UX 符合"两次 ESC 关 GUI"通行约定 |

## 后续跟进

- 弹药合成 owner 绑定在玩家永久离线（被 ban / 删号）时产物永久卡住，需破坏方块才能回收。如需改进，可增设"超时释放 owner"机制（例如 owner 离线 > 10 分钟后清空）。
- 如果后续增加蓝图类型，需在 `WeaponWorkbenchMenu.quickMoveStack` 的 `isBlueprint` 分类中加入新 item。
