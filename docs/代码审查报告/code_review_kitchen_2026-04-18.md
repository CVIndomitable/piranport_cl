# 皮兰港 v0.0.11-alpha 农业与美食代码审查 · 修复记录

**审查日期**：2026-04-18
**审查范围**：作物/稻田/盐蒸发/盐矿生成 + 烹饪锅/石磨/砧板/可放置食物/食物物品/配方/酿造/菜单
**用户决定**：10 项中修复 10 项（全修）

---

## 已修复问题

### P1

1. `CookingPotBlockEntity` 烹饪进度保存了但加载后被强制重置
   - 修复：`saveAdditional` 序列化 `currentRecipeId`，`loadAdditional` 反序列化；`serverTick` 追加分支「`currentRecipe == null && recipeId 匹配` → 仅补上 recipe 引用，保留 progress」，避免重启后原本煮到一半的菜归零。

2. `StoneMillBlockEntity` 加载后不会自动恢复处理
   - 修复：新增 `needsReprocess` 字段（默认 true）+ `serverTick` 静态入口；`StoneMillBlock.getTicker` 注册 ticker，首次 tick 调用 `processRecipes()` 后置 false。服务器重启或输出槽满→被取走的场景下不再停滞。

3. `CookingPotMenu.fromNetwork` 找不到 BE 时抛异常导致客户端炸 screen
   - 修复：`blockEntity` 字段改 `@Nullable`，BE 为空时 fallback 到 `ItemStackHandler(TOTAL_SLOTS)` + `SimpleContainerData(2)`，与 `StoneMillMenu` 对齐；`stillValid` 在 null 时直接返回 false。

### P2

4. `CuttingBoardBlock.useItemOn` 创造模式也消耗手持物品
   - 修复：`stack.split(1)` 改为 `stack.copyWithCount(1)` + 显式 `if (!player.isCreative()) stack.shrink(1)`。

5. `SaltEvaporationHandler` 的蒸发进度不持久化
   - 修复：新增 `SaltEvaporationData extends SavedData`，迁移 per-dimension `Map<BlockPos, Integer>` 到世界存档（`piranport_salt_evaporation.dat`）。每次 put / 倒计时更新 / 条件失效移除都调 `setDirty()`。原本的 `onLevelUnload` / `onServerStopped` 清理订阅不再需要，已移除。

6. `BottleFoodItem.finishUsingItem` 玩家背包满时玻璃瓶丢失
   - 修复：`inventory.add` 返回 false 时调用 `player.drop(bottle, false)` 掉落到世界。

7. `PlaceableFoodBlockEntity.eat` 的 ceil 分配让累计 nutrition/duration 超出原食物
   - 修复：改用累计整数分配 `(nutrition * (biteIdx+1) / servings) - (nutrition * biteIdx / servings)`，保证 Σ(bites) == 原值；MobEffect 只在最后一口按 `pe.probability()` 单次 roll 并以完整 duration 添加，与原版单次食用语义一致。

### P3

8. `CuttingBoardBlock` 未预检「可切割物品」
   - 修复：`useItemOn` 在放置前用 `RecipeManager.getRecipeFor(CUTTING_BOARD_TYPE, SingleRecipeInput(stack), level)` 过滤，无匹配 recipe 则 `PASS_TO_DEFAULT_BLOCK_INTERACTION`，防止把刀/工具误放砧板。

9. `StoneMillRecipe.matches` / `consumeIngredients` 与 `CookingPotRecipe` 行为不一致
   - 修复：两处都改为允许单 slot 的 stack count 满足多个同类 ingredient（参照烹饪锅实现），玩家不再需要把同一原料拆到多个槽位。

10. `SaltEvaporationHandler` 对 NeighborNotifyEvent 过度响应
    - 修复：`onBlockUpdate` 先按 `event.getState()` 过滤，仅在 source 是 `Blocks.WATER` 或 `AbstractFurnaceBlock` 时才 `tryTrack`，减少高频事件下无谓的 BlockState 查询。

---

## 涉及文件

- `src/main/java/com/piranport/block/entity/CookingPotBlockEntity.java` — recipeId 持久化 + load 恢复分支
- `src/main/java/com/piranport/block/entity/StoneMillBlockEntity.java` — `serverTick` + `needsReprocess` + consume 对齐
- `src/main/java/com/piranport/block/entity/PlaceableFoodBlockEntity.java` — 累计分配 + 最后一口 roll effect
- `src/main/java/com/piranport/block/StoneMillBlock.java` — 注册 ticker
- `src/main/java/com/piranport/block/CuttingBoardBlock.java` — recipe 预检 + 创造模式不消耗
- `src/main/java/com/piranport/block/SaltEvaporationHandler.java` — 改走 SavedData + 事件过滤
- `src/main/java/com/piranport/block/SaltEvaporationData.java` — 新增，per-dimension 蒸发状态持久化
- `src/main/java/com/piranport/menu/CookingPotMenu.java` — nullable BE 容错
- `src/main/java/com/piranport/item/BottleFoodItem.java` — 背包满 drop fallback
- `src/main/java/com/piranport/recipe/StoneMillRecipe.java` — matches 对齐烹饪锅
