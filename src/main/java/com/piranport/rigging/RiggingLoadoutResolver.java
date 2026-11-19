package com.piranport.rigging;

import com.piranport.aviation.AircraftIndex;
import com.piranport.combat.TransformationManager;
import com.piranport.entity.AircraftEntity;
import com.piranport.skin.SkinManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 从快捷栏与在飞飞机推导舰装挂点分配。服务端同步结果；客户端兜底只扫描快捷栏。
 *
 * <p>服务端在每个玩家的 tick 里调用它生成 {@link RiggingLoadout} 并同步；
 * 客户端只在「本地玩家尚未收到快照」时用它兜底（进服第一帧、单人模式）。
 */
public final class RiggingLoadoutResolver {

    /** 只扫描快捷栏 0~8：背包里的武器不挂到舰装上。 */
    public static final int HOTBAR_SIZE = 9;

    private static final List<RiggingSocket> WAIST_SOCKETS =
            List.of(RiggingSocket.WAIST_LEFT, RiggingSocket.WAIST_RIGHT);
    /** 无甲板时供所有舷侧武器使用；有甲板时仅主手能占用右前。 */
    private static final List<RiggingSocket> SIDE_ORDER =
            List.of(RiggingSocket.SIDE_RIGHT_2, RiggingSocket.SIDE_LEFT_2,
                    RiggingSocket.SIDE_RIGHT_1, RiggingSocket.SIDE_LEFT_1);
    private static final List<RiggingSocket> CARRIER_SIDE_ORDER =
            List.of(RiggingSocket.SIDE_LEFT_2, RiggingSocket.SIDE_LEFT_1);
    private static final List<RiggingSocket> DECK_SOCKETS =
            List.of(RiggingSocket.DECK_1, RiggingSocket.DECK_2);
    private static final List<RiggingSocket> BACK_SOCKETS =
            List.of(RiggingSocket.BACK_1, RiggingSocket.BACK_2);

    private RiggingLoadoutResolver() {}

    /** 服务端入口：皮肤 ID 走已同步的 ACTIVE_SKIN 附件。 */
    public static RiggingLoadout resolve(Player player) {
        return resolve(player, SkinManager.getActiveSkin(player));
    }

    /**
     * 客户端兜底入口：皮肤 ID 由调用方给（客户端玩家皮肤在
     * {@code ClientSkinData} 里，不在 ACTIVE_SKIN 附件里）。
     */
    public static RiggingLoadout resolve(Player player, int skinId) {
        return resolve(player, skinId, false);
    }

    /** Clients can preserve an airborne deck from the synced snapshot while using their current hotbar. */
    public static RiggingLoadout resolve(Player player, int skinId, boolean syncedDeck) {
        if (player == null) return RiggingLoadout.EMPTY;
        // 未变身就不挂任何东西；否则挂点会残留在解除变身后的玩家身上。
        if (TransformationManager.findTransformedCore(player).isEmpty()) {
            return RiggingLoadout.EMPTY;
        }
        boolean airborne = player.level().isClientSide() && syncedDeck;
        boolean returning = false;
        if (!player.level().isClientSide()) {
            for (AircraftEntity craft : AircraftIndex.snapshot(player.getUUID())) {
                if (craft.isRemoved() || !craft.isAlive() || craft.level() != player.level()) continue;
                airborne = true;
                if (craft.getFlightState() == AircraftEntity.FlightState.RETURNING) {
                    returning = true;
                    break;
                }
            }
        }
        return assign(player.getInventory(), skinId, airborne, returning);
    }

    /**
     * 挂点分配规则（只依赖快捷栏内容，便于单元测试与两端复用）：
     * <ol>
     *   <li>主手是主炮、反舰导弹或飞机时，该槽最先占用同类挂点；其余按快捷栏 0→8 顺序</li>
     *   <li>鱼雷 → 腰部左、右</li>
     *   <li>主炮 / 反舰导弹 / 火箭弹 → 按固定顺序占舷侧炮位：
     *       <b>右前 → 左前 → 右后 → 左后</b>。主手是主炮/反舰导弹时该槽排最前，
     *       始终落在右前角</li>
     *   <li>有任意舰载机时飞行甲板替代右侧炮架，飞机 → 甲板两个位；
     *       主手以外的舷侧武器只分配左侧两个炮位</li>
     *   <li>深弹 / 防空导弹 → 后背两个位</li>
     *   <li>超出容量的物品直接丢弃（物品栏、数值、开火都不受影响）</li>
     * </ol>
     */
    public static RiggingLoadout assign(Inventory inventory, int skinId) {
        return assign(inventory, skinId, false);
    }

    public static RiggingLoadout assign(Inventory inventory, int skinId, boolean airborne) {
        return assign(inventory, skinId, airborne, false);
    }

    static RiggingLoadout assign(Inventory inventory, int skinId, boolean airborne, boolean returning) {
        int selectedSlot = inventory.selected;

        List<Candidate> candidates = new ArrayList<>();
        boolean hasAircraft = airborne;
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack stack = inventory.items.get(slot);
            RiggingCategory category = RiggingCategory.resolve(stack);
            if (category == null) continue;
            candidates.add(new Candidate(slot, category, stack));
            if (category == RiggingCategory.AIRCRAFT) {
                hasAircraft = true;
            }
        }

        // 各类别的挂点独立计数，提前主手只会改变它所在类别的分配。
        RiggingCategory selectedCategory = selectedSlot >= 0 && selectedSlot < HOTBAR_SIZE
                ? RiggingCategory.resolve(inventory.items.get(selectedSlot))
                : null;
        if (selectedCategory == RiggingCategory.CANNON || selectedCategory == RiggingCategory.MISSILE_ASM
                || selectedCategory == RiggingCategory.AIRCRAFT) {
            candidates.sort(Comparator.comparingInt(candidate ->
                    candidate.slot() == selectedSlot ? -1 : candidate.slot()));
        }

        List<RiggingSocket> sideOrder = sideSockets(hasAircraft);
        List<RiggingMount> mounts = new ArrayList<>();
        int waist = 0;
        int side = 0;
        int deck = 0;
        int back = 0;
        for (Candidate candidate : candidates) {
            RiggingSocket socket = switch (candidate.category()) {
                case TORPEDO -> waist < WAIST_SOCKETS.size() ? WAIST_SOCKETS.get(waist++) : null;
                case CANNON, MISSILE_ASM -> hasAircraft && candidate.slot() == selectedSlot
                        ? RiggingSocket.SIDE_RIGHT_2
                        : side < sideOrder.size() ? sideOrder.get(side++) : null;
                case AIRCRAFT -> hasAircraft && deck < DECK_SOCKETS.size()
                        ? DECK_SOCKETS.get(deck++)
                        : null;
                case DEPTH_CHARGE, MISSILE_SAM -> back < BACK_SOCKETS.size()
                        ? BACK_SOCKETS.get(back++)
                        : null;
            };
            if (socket != null) {
                mounts.add(new RiggingMount(socket, candidate.category(), candidate.stack(), candidate.slot()));
            }
        }

        int hullProfile = hullProfileForSkin(skinId, hasAircraft);
        return new RiggingLoadout(mounts, hasAircraft, hullProfile, selectedSlot, returning);
    }

    /**
     * 皮肤 → 舰体轮廓。原先是客户端的私有判定，服务端同步快照需要同一结果，故下沉到这里。
     */
    public static int hullProfileForSkin(int skinId, boolean hasAircraft) {
        return switch (skinId) {
            case 4, 18, 20 -> RiggingLoadout.HULL_CARRIER;
            case 5, 6, 7, 8, 9, 13, 14, 15, 21, 22 -> RiggingLoadout.HULL_DESTROYER;
            case 10, 11, 12, 16, 19 -> RiggingLoadout.HULL_CRUISER;
            case 17 -> RiggingLoadout.HULL_SUBMARINE;
            default -> hasAircraft ? RiggingLoadout.HULL_CARRIER : RiggingLoadout.HULL_DEFAULT;
        };
    }

    public static List<RiggingSocket> sideSockets(boolean hasAircraft) {
        return hasAircraft ? CARRIER_SIDE_ORDER : SIDE_ORDER;
    }

    private record Candidate(int slot, RiggingCategory category, ItemStack stack) {}
}
