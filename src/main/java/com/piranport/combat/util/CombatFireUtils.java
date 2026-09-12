package com.piranport.combat.util;

import com.piranport.item.TorpedoItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 武器开火共享辅助方法。
 *
 * <p>从 ShipCoreCombat 提取的跨包共享工具，供各 FireStrategy 子系统复用。
 * 保持纯静态、无状态，所有方法线程安全（无共享可变状态）。
 *
 * <p>职责：
 * <ul>
 *   <li>鱼雷类型判定（磁性/线导/声自导）— ItemStack 与 ResourceLocation 双形式</li>
 *   <li>散布角度表（齐射枪管间偏移）</li>
 *   <li>水平方向向量绕 Y 轴旋转（散布/扇面计算）</li>
 * </ul>
 */
public final class CombatFireUtils {

    private CombatFireUtils() {}

    // ===== 鱼雷类型判定 =====

    public static boolean isMagneticTorpedo(ItemStack stack) {
        return stack.getItem() instanceof TorpedoItem ti && ti.isMagnetic();
    }

    public static boolean isMagneticTorpedo(String ammoItemId) {
        Item item = resolveItem(ammoItemId);
        return item instanceof TorpedoItem ti && ti.isMagnetic();
    }

    public static boolean isWireGuidedTorpedo(ItemStack stack) {
        return stack.getItem() instanceof TorpedoItem ti && ti.isWireGuided();
    }

    public static boolean isWireGuidedTorpedo(String ammoItemId) {
        Item item = resolveItem(ammoItemId);
        return item instanceof TorpedoItem ti && ti.isWireGuided();
    }

    public static boolean isAcousticTorpedo(ItemStack stack) {
        return stack.getItem() instanceof TorpedoItem ti && ti.isAcoustic();
    }

    public static boolean isAcousticTorpedo(String ammoItemId) {
        Item item = resolveItem(ammoItemId);
        return item instanceof TorpedoItem ti && ti.isAcoustic();
    }

    private static Item resolveItem(String ammoItemId) {
        ResourceLocation rl = ResourceLocation.tryParse(ammoItemId);
        if (rl == null) return null;
        return BuiltInRegistries.ITEM.get(rl);
    }

    // ===== 散布角度 =====

    /**
     * 返回多管齐射时各枪管的相对角度偏移（度数）。
     * count=1→0，2→±3，3→-4/0/4，4→-6/-2/2/6。
     */
    public static float[] getSpreadAngles(int count) {
        return switch (count) {
            case 2 -> new float[]{-3f, 3f};
            case 3 -> new float[]{-4f, 0f, 4f};
            case 4 -> new float[]{-6f, -2f, 2f, 6f};
            default -> new float[]{0f};
        };
    }

    // ===== 几何 =====

    /**
     * 将水平方向向量绕 Y 轴旋转 angleRad 弧度。
     * 投影到水平面后归一化（零向量回退到 +X）。
     */
    public static Vec3 rotateHorizontal(Vec3 look, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double nx = look.x * cos - look.z * sin;
        double nz = look.x * sin + look.z * cos;
        Vec3 result = new Vec3(nx, 0, nz);
        return result.lengthSqr() > 0 ? result.normalize() : new Vec3(1, 0, 0);
    }
}