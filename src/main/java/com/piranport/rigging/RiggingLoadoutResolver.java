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

    private static final List<RiggingSocket> WAIST_SOCKETS =
            List.of(RiggingSocket.WAIST_LEFT, RiggingSocket.WAIST_RIGHT);
    /**
     * 舷侧炮位的分配顺序：<b>右前 → 左前 → 右后 → 左后</b>。
     *
     * <p>WHY：舰装容量有限，只会显示其中几个炮位；候选列表已把当前选中槽排最前，
     * 按这个顺序分配，主手武器一定落在最显眼的右前角 {@code SIDE_RIGHT_2}。
     * 用户实机确认：玩家模型 +X 是左、+Z 是后，所以 x=-9.5、z=-6 的
     * {@code SIDE_RIGHT_2} 才是右前；旧顺序把主手放到了左后 {@code SIDE_LEFT_1}。
     *
     * <p>甲板现在挂在右臂之外，右臂平台仍然存在，所以有飞机时也按同一顺序分配，
     * 不再禁用右舷炮位（{@code hasAircraft} 只决定是否叠加甲板模型与飞机挂点）。
     */
    private static final List<RiggingSocket> SIDE_ORDER =
            List.of(RiggingSocket.SIDE_RIGHT_2, RiggingSocket.SIDE_LEFT_2,
                    RiggingSocket.SIDE_RIGHT_1, RiggingSocket.SIDE_LEFT_1);
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
     *   <li>主手是主炮 / 反舰导弹时，该槽最先占位；其余情况（主手是鱼雷、深弹、
     *       防空导弹或飞机）不特殊提前，所有武器按快捷栏 0→8 顺序</li>
     *   <li>鱼雷 → 腰部左、右</li>
     *   <li>主炮 / 反舰导弹 / 火箭弹 → 按固定顺序占舷侧炮位：
     *       <b>右前 → 左前 → 右后 → 左后</b>。主手是主炮/反舰导弹时该槽排最前，
     *       因此主手固定落在右前角的 {@code SIDE_RIGHT_2}</li>
     *   <li>有任意舰载机时叠加飞行甲板（在右臂之外），飞机 → 甲板两个位；
     *       右臂炮位仍然可用，不再禁用右舷</li>
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

        // 只有主手是主炮/反舰导弹时才把它排到最前。
        // WHY：主炮/反舰导弹是「看得见的主武器」，主手必须出现在最前面的舷侧炮位；
        // 鱼雷/深弹/防空导弹走各自的分类挂点（腰部/背部），按自然顺序即可，
        // 否则主手一换整套挂点就会重排，观感上很乱。
        RiggingCategory selectedCategory = selectedSlot >= 0 && selectedSlot < HOTBAR_SIZE
                ? RiggingCategory.resolve(inventory.items.get(selectedSlot))
                : null;
        boolean selectedIsSurfaceWeapon = selectedCategory == RiggingCategory.CANNON
                || selectedCategory == RiggingCategory.MISSILE_ASM;
        if (selectedIsSurfaceWeapon) {
            // 选中槽排到最前；其余保持快捷栏顺序（List.sort 稳定）。
            candidates.sort(Comparator.comparingInt(candidate ->
                    candidate.slot() == selectedSlot ? -1 : candidate.slot()));
        }

        List<RiggingMount> mounts = new ArrayList<>();
        int waist = 0;
        int side = 0;
        int deck = 0;
        int back = 0;
        for (Candidate candidate : candidates) {
            RiggingSocket socket = switch (candidate.category()) {
                case TORPEDO -> waist < WAIST_SOCKETS.size() ? WAIST_SOCKETS.get(waist++) : null;
                case CANNON, MISSILE_ASM ->
                        side < SIDE_ORDER.size() ? SIDE_ORDER.get(side++) : null;
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
