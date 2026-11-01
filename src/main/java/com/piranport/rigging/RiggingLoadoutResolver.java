package com.piranport.rigging;

import com.piranport.combat.TransformationManager;
import com.piranport.skin.SkinManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 从快捷栏推导舰装挂点分配。服务端与客户端共用同一套规则。
 *
 * <p>服务端在每个玩家的 tick 里调用它生成 {@link RiggingLoadout} 并同步；
 * 客户端只在「本地玩家尚未收到快照」时用它兜底（进服第一帧、单人模式）。
 */
public final class RiggingLoadoutResolver {

    /** 只扫描快捷栏 0~8：背包里的武器不挂到舰装上。 */
    public static final int HOTBAR_SIZE = 9;

    private static final List<RiggingSocket> LEG_SOCKETS =
            List.of(RiggingSocket.LEG_LEFT, RiggingSocket.LEG_RIGHT);
    private static final List<RiggingSocket> LEFT_SIDE_SOCKETS =
            List.of(RiggingSocket.SIDE_LEFT_1, RiggingSocket.SIDE_LEFT_2);
    private static final List<RiggingSocket> RIGHT_SIDE_SOCKETS =
            List.of(RiggingSocket.SIDE_RIGHT_1, RiggingSocket.SIDE_RIGHT_2);
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
        if (player == null) return RiggingLoadout.EMPTY;
        // 未变身就不挂任何东西；否则挂点会残留在解除变身后的玩家身上。
        if (TransformationManager.findTransformedCore(player).isEmpty()) {
            return RiggingLoadout.EMPTY;
        }
        return assign(player.getInventory(), skinId);
    }

    /**
     * 挂点分配规则（只依赖快捷栏内容，便于单元测试与两端复用）：
     * <ol>
     *   <li>当前选中的快捷栏武器最先占位，其余按快捷栏 0→8 顺序</li>
     *   <li>鱼雷 → 左腿、右腿</li>
     *   <li>主炮 / 反舰导弹 / 火箭弹 → 左舷两个位，再右舷两个位</li>
     *   <li>有任意舰载机时右舷整侧改成飞行甲板，右舷炮位取消，飞机 → 甲板两个位</li>
     *   <li>深弹 / 防空导弹 → 后背两个位</li>
     *   <li>超出容量的物品直接丢弃（物品栏、数值、开火都不受影响）</li>
     * </ol>
     */
    public static RiggingLoadout assign(Inventory inventory, int skinId) {
        int selectedSlot = inventory.selected;

        List<Candidate> candidates = new ArrayList<>();
        boolean hasAircraft = false;
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack stack = inventory.items.get(slot);
            RiggingCategory category = RiggingCategory.resolve(stack);
            if (category == null) continue;
            candidates.add(new Candidate(slot, category, stack));
            if (category == RiggingCategory.AIRCRAFT) {
                hasAircraft = true;
            }
        }

        // 选中槽排到最前；其余保持快捷栏顺序（List.sort 稳定）。
        candidates.sort(Comparator.comparingInt(candidate ->
                candidate.slot() == selectedSlot ? -1 : candidate.slot()));

        List<RiggingMount> mounts = new ArrayList<>();
        int leg = 0;
        int leftSide = 0;
        int rightSide = 0;
        int deck = 0;
        int back = 0;
        for (Candidate candidate : candidates) {
            RiggingSocket socket = switch (candidate.category()) {
                case TORPEDO -> leg < LEG_SOCKETS.size() ? LEG_SOCKETS.get(leg++) : null;
                case CANNON, MISSILE_ASM -> {
                    if (leftSide < LEFT_SIDE_SOCKETS.size()) {
                        yield LEFT_SIDE_SOCKETS.get(leftSide++);
                    }
                    // 带飞机时右舷没有炮位：整侧换成了飞行甲板。
                    yield !hasAircraft && rightSide < RIGHT_SIDE_SOCKETS.size()
                            ? RIGHT_SIDE_SOCKETS.get(rightSide++)
                            : null;
                }
                case AIRCRAFT -> hasAircraft && deck < DECK_SOCKETS.size()
                        ? DECK_SOCKETS.get(deck++)
                        : null;
                case DEPTH_CHARGE, MISSILE_SAM -> back < BACK_SOCKETS.size()
                        ? BACK_SOCKETS.get(back++)
                        : null;
            };
            if (socket != null) {
                mounts.add(new RiggingMount(socket, candidate.category(), candidate.stack()));
            }
        }

        int hullProfile = hullProfileForSkin(skinId, hasAircraft);
        return new RiggingLoadout(mounts, hasAircraft, hullProfile, selectedSlot);
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

    private record Candidate(int slot, RiggingCategory category, ItemStack stack) {}
}
