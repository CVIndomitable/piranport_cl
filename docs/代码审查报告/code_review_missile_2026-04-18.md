# 皮兰港 v0.0.11-alpha 导弹系统代码审查 · 修复记录

**审查日期**：2026-04-18
**审查范围**：`MissileEntity.java` / `MissileLauncherItem.java` / `MissileItem.java` + `ShipCoreItem.java` 中导弹相关方法
**用户决定**：11 项中修复 11 项（第 11 项"联装数"是海军术语，非描述错误，不修）

---

## 已修复问题

### P1

1. `MissileLauncherItem.use()` 在冷却阻断时仍返回 `consume`
   - 修复：`fireWeaponAtSlot` 改为返回 `boolean`（冷却阻断时返回 `false`），由 `tryFireFromInventory` 透传；所有现有 `if (tryFireFromInventory(...)) consume else pass` 调用点无需改动，但现在冷却时正确走 `pass` 分支。

2. `MissileEntity.setTrackedTarget(null)` 会永久禁用自动搜索
   - 修复：`this.manualTarget = (target != null);`，传 null 会清目标并恢复自动搜索。

### P2

3. `MissileType` 以 `ordinal()` 存盘
   - 修复：`tag.putString("MissileType", missileType.name())`；读取时优先按字符串 `valueOf` 解析，旧存档的 int ordinal 仍兼容（`readMissileType` 方法）。

4. `AP_PEN_ID` 静态 ID 在并发/嵌套命中下会抛 `IllegalArgumentException`
   - 修复：每次命中基于 `getUUID()` 生成唯一 ID `missile_ap/<entityUUID>`；调用 `removeModifier` 前兜底，避免同一导弹理论重入风险。

5. `canHitEntity` 未过滤 `Container`
   - 修复：在 `canHitEntity` 中加入 `Container` 过滤，与 `findTarget` / `spawnMissileAutoAim` 一致。

6. `spawnMissile` 初始 Y 不随仰角偏移
   - 修复：抽取 `spawnMissileWithDir(...)` 统一生成点计算，Y 位置加 `d.y * 0.5`。

7. `onHitBlock` 未调用 `super.onHitBlock`
   - 修复：`super.onHitBlock(result);` 放在最前。

### P3

8. `getDefaultItem()` 每次调用都做注册表查询
   - 修复：新增 `cachedDisplayItem` 字段，首次 resolve 后缓存；`readAdditionalSaveData` 将缓存清空以触发重新解析。

9. `findTarget` 水下判断重复
   - 修复：水下/地面过滤全部下沉到 `level().getEntities(...)` 的 predicate 中，循环体只保留距离比较。

10. `MissileAmmoType` 与 `MissileType` 双枚举
    - 修复：删除 `MissileAmmoType`，在 `MissileType` 上追加 `translationKey` / `color` 两个字段；`MissileItem` 构造签名改为接收 `MissileType`；`ModItems.java` 所有引用同步更新。

11. (跳过 —— 联装数 burstCount 是海军术语 · 用户决定)

12. `spawnMissile` / `spawnMissileAutoAim` 重复
    - 修复：`spawnMissileAutoAim` 调用 `spawnMissileWithDir`，消除重复的 setOwner / setPos / setDeltaMovement 逻辑。

---

## 涉及文件

- `src/main/java/com/piranport/entity/MissileEntity.java` — 核心实体，多项修复
- `src/main/java/com/piranport/item/MissileLauncherItem.java` — switch 简化为字段引用
- `src/main/java/com/piranport/item/MissileItem.java` — 改用 MissileType，删除 MissileAmmoType
- `src/main/java/com/piranport/item/ShipCoreItem.java` — `tryFireFromInventory` / `fireWeaponAtSlot` 返回 bool；`spawnMissile` / `spawnMissileAutoAim` 抽 helper
- `src/main/java/com/piranport/registry/ModItems.java` — 构造签名从 `MissileAmmoType` 改为 `MissileType`

## 编译验证

`./gradlew compileJava` 通过，仅保留 2 条与本次改动无关的 `EventBusSubscriber.Bus` deprecation 警告。
