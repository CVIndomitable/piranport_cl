package com.piranport.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 火控目标存活判定 —— 服务端清理与客户端显示共用同一口径。
 *
 * <p>WHY 不能只看 {@code isRemoved()}：深海舰船死亡后要走下沉动画，实体在
 * {@code die()} 之后仍会留在世界中若干 tick。只判「是否被移除」会在这段窗口里
 * 继续把尸体当目标，HUD 行、红框、落点预瞄都挂在一具正在下沉的尸体上。</p>
 *
 * <p>WHY 也不能只看 {@code isAlive()}：{@code LivingEntity.isAlive()} 的语义是
 * 「未移除且血量 &gt; 0」，而死亡标志（{@code die()} 里的 {@code dead = true}）与
 * 血量归零并不总是同一时刻；脚本/直接调用 {@code die()} 的路径可能出现
 * 「死亡标志已置、血量尚未归零」。因此把「存活」与「已死亡/正在死亡」一起判。</p>
 *
 * <p>{@code AircraftEntity} 不是 {@code LivingEntity}，没有 {@code isDeadOrDying()}，
 * 这类目标只按实体级的「存在 + 未移除 + 存活」判定。</p>
 */
public final class FireControlTargetState {

    private FireControlTargetState() {
        // 纯静态工具类，禁止实例化
    }

    /**
     * 纯状态判定，便于单测：四个条件全部满足才算仍是有效火控目标。
     *
     * @param present     实体是否存在（null 视为不存在）
     * @param removed     是否已被移除
     * @param alive       是否存活
     * @param deadOrDying 是否已死亡/正在死亡
     */
    public static boolean trackable(boolean present, boolean removed, boolean alive, boolean deadOrDying) {
        return present && !removed && alive && !deadOrDying;
    }

    /** @return true 表示该实体仍可作为火控目标被显示与追踪 */
    public static boolean trackable(@Nullable Entity entity) {
        if (entity == null) return false;
        boolean deadOrDying = entity instanceof LivingEntity living && living.isDeadOrDying();
        return trackable(true, entity.isRemoved(), entity.isAlive(), deadOrDying);
    }

    /** @return true 表示该目标已失效（不存在/已移除/已死亡/正在死亡） */
    public static boolean lost(@Nullable Entity entity) {
        return !trackable(entity);
    }
}
