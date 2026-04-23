package com.piranport.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.compat.maid.client.MaidWeaponIconRenderer;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class MaidCompatClient {
    private MaidCompatClient() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(MaidCompatClient::onRenderMaid);
    }

    private static void onRenderMaid(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof EntityMaid maid)) return;
        MaidWeaponIconRenderer.render(maid, event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight());
    }
}
