package com.piranport.npc.deepocean;

import com.piranport.dungeon.escort.EscortRoute;
import com.piranport.npc.ai.goal.EscortSailGoal;
import com.piranport.npc.ai.goal.FollowLeaderGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 深海运输船 — 无武装的箱船，随编队行动或单独漂航。
 * HP:120 Armor:4 Speed:0.15（约 3 米/秒）Detection:32
 *
 * <p>依据《深海/01》§九：运输船没有攻击手段，因此不注册任何 targetSelector，
 * 只保留跟随领舰与闲逛；被攻击时也不会反击。</p>
 *
 * <p>护航模式：设置了 {@link #setEscortTarget(Vec3)} 后，{@link EscortSailGoal} 优先级 0 接管移动，
 * 按航线逐个路径点以 0.15 格/tick 驶向目标，压过跟队与闲逛；无航线时直指目标。</p>
 */
public class DeepOceanTransportEntity extends AbstractDeepOceanEntity {

    private static final String TAG_ESCORT_X = "EscortX";
    private static final String TAG_ESCORT_Y = "EscortY";
    private static final String TAG_ESCORT_Z = "EscortZ";

    @Nullable
    private Vec3 escortTarget;

    /** 运行时航线，不落盘：重启或区块卸载后由副本侧按 {@link #escortTarget} 重新规划。 */
    @Nullable
    private EscortRoute escortRoute;

    public DeepOceanTransportEntity(EntityType<? extends DeepOceanTransportEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new EscortSailGoal(this));
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.10));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.6, 32));
    }

    @Override
    protected boolean attacksEscortTransports() {
        return false;
    }

    public void setEscortTarget(@Nullable Vec3 target) {
        this.escortTarget = target;
    }

    @Nullable
    public Vec3 getEscortTarget() {
        return escortTarget;
    }

    @Nullable
    public EscortRoute getEscortRoute() {
        return escortRoute;
    }

    public void setEscortRoute(@Nullable EscortRoute route) {
        this.escortRoute = route;
    }

    /** 当前应驶向的点：有航线时是下一个路径点，否则直接指向原目标。 */
    @Nullable
    public Vec3 getSteerTarget() {
        if (escortTarget == null) return null;
        return escortRoute != null ? escortRoute.currentWaypoint(getX(), getZ()) : escortTarget;
    }

    /** 航线终点就是抵达判定点：目标可通行时即原目标，否则为最近的可通行点。 */
    public boolean hasArrived() {
        if (escortTarget == null) return false;
        Vec3 arrival = escortRoute != null ? escortRoute.end() : escortTarget;
        return EscortSailGoal.containsXZ(getBoundingBox(), arrival);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (escortTarget != null) {
            tag.putDouble(TAG_ESCORT_X, escortTarget.x);
            tag.putDouble(TAG_ESCORT_Y, escortTarget.y);
            tag.putDouble(TAG_ESCORT_Z, escortTarget.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_ESCORT_X)) {
            escortTarget = new Vec3(tag.getDouble(TAG_ESCORT_X), tag.getDouble(TAG_ESCORT_Y), tag.getDouble(TAG_ESCORT_Z));
        }
    }

    /** 运输船不参与环绕交战；JSON 缺失时也保持 0，避免误走环绕逻辑。 */
    @Override
    public double getOrbitDistance() {
        DeepOceanEntityData data = getJsonData();
        return data != null ? data.orbitDistance() : 0.0;
    }

    @Override public boolean canUseTorpedoes() { return false; }
    @Override public boolean canLaunchAircraft() { return false; }
    @Override public int getMaxAircraft() { return 0; }
}
