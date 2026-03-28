package com.piranport.aviation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client-side recon mode state. */
@OnlyIn(Dist.CLIENT)
public class ClientReconData {
    private static boolean inReconMode = false;
    private static int reconEntityId = -1;

    public static void setReconActive(int entityId) {
        inReconMode = true;
        reconEntityId = entityId;
    }

    public static void clearRecon() {
        inReconMode = false;
        reconEntityId = -1;
    }

    public static boolean isInReconMode() { return inReconMode; }
    public static int getReconEntityId() { return reconEntityId; }
}
