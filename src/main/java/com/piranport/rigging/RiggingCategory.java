package com.piranport.rigging;

import com.piranport.artillery.ArtilleryItem;
import com.piranport.entity.MissileEntity;
import com.piranport.item.AircraftItem;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoLauncherItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 舰装挂点可承载的装备大类。
 *
 * <p>分类只服务挂点分配与占位渲染，不参与任何开火判定 —— 开火仍走原逻辑。
 */
public enum RiggingCategory {
    /** 主炮（07 表火炮） */
    CANNON,
    /** 鱼雷发射器 */
    TORPEDO,
    /** 反舰导弹与火箭弹：走两侧舷装挂点 */
    MISSILE_ASM,
    /** 防空导弹：走后背挂点（策划决策：ANTI_AIR 去后背，其余去两侧） */
    MISSILE_SAM,
    /** 深水炸弹投射器：走后背挂点 */
    DEPTH_CHARGE,
    /** 舰载机：只有带上飞机时才在右舷甲板出现挂点 */
    AIRCRAFT;

    /** 缓存 values()，避免每次分类都克隆数组。 */
    private static final RiggingCategory[] VALUES = values();

    /**
     * 按物品类型归类；不是舰装武器时返回 null。
     *
     * <p>防空导弹与反舰/火箭弹共用 {@link MissileLauncherItem}，只能靠 {@code getMissileType()} 区分。
     */
    @Nullable
    public static RiggingCategory resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.getItem() instanceof ArtilleryItem) return CANNON;
        if (stack.getItem() instanceof TorpedoLauncherItem) return TORPEDO;
        if (stack.getItem() instanceof MissileLauncherItem launcher) {
            return launcher.getMissileType() == MissileEntity.MissileType.ANTI_AIR ? MISSILE_SAM : MISSILE_ASM;
        }
        if (stack.getItem() instanceof DepthChargeLauncherItem) return DEPTH_CHARGE;
        if (stack.getItem() instanceof AircraftItem) return AIRCRAFT;
        return null;
    }

    /** 网络序号的合法化读取；越界说明对端数据损坏，直接抛错而不是静默降级。 */
    public static RiggingCategory byId(int id) {
        if (id < 0 || id >= VALUES.length) {
            throw new IllegalArgumentException("Unknown rigging category id: " + id);
        }
        return VALUES[id];
    }

    public static int count() {
        return VALUES.length;
    }
}
