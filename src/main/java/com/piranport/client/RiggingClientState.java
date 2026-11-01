package com.piranport.client;

import com.piranport.combat.TransformationManager;
import com.piranport.registry.ModAttachmentTypes;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingLoadoutResolver;
import com.piranport.skin.ClientSkinData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * 舰装渲染层共用的客户端状态读取。
 *
 * <p>占位层（{@link ShipRiggingLayer}）和 GeckoLib 层都必须走同一套「谁是变身玩家、
 * 挂点快照从哪来」的判断，否则两层的显示会不一致（比如占位层已经消失、GeckoLib 层还没出现）。
 *
 * <p><b>兼容说明（Yes Steve Model 等）</b>：玩家启用 YSM 模型时，其 mixin 会在
 * {@code EntityRenderDispatcher} 层直接跳过原版 {@code PlayerRenderer.render}，整棵 RenderLayer
 * 树都不执行，因此舰装层、皮肤叠层会自动不渲染 —— 这是预期的兼容行为，不需要（也没有稳定
 * API 可以）在这里加 YSM 专用开关。未启用 YSM 模型的玩家仍走原版渲染，舰装正常显示。
 */
public final class RiggingClientState {

    private RiggingClientState() {}

    /**
     * 远程玩家只认同步快照；本地玩家在没有快照时按背包现场算一份。
     *
     * <p>{@link RiggingLoadout#isEmpty()} 用的是 selectedSlot = -1 哨兵，
     * 所以「已变身但快捷栏里没武器」不会被误判成没有快照。
     */
    public static RiggingLoadout resolveLoadout(AbstractClientPlayer player) {
        RiggingLoadout synced = player.getData(ModAttachmentTypes.RIGGING_LOADOUT.get());
        if (!synced.isEmpty()) return synced;

        boolean localPlayer = player == Minecraft.getInstance().player;
        if (localPlayer && TransformationManager.isPlayerTransformed(player)) {
            return RiggingLoadoutResolver.resolve(player, ClientSkinData.getActiveSkin(player.getUUID()));
        }
        return synced;
    }

    /** 本地玩家看变身状态；远程玩家只能靠已同步的皮肤 ID 判断。 */
    public static boolean isRenderedAsTransformed(AbstractClientPlayer player) {
        boolean localPlayer = player == Minecraft.getInstance().player;
        return localPlayer
                ? TransformationManager.isPlayerTransformed(player)
                : ClientSkinData.getActiveSkin(player.getUUID()) > 0;
    }
}
