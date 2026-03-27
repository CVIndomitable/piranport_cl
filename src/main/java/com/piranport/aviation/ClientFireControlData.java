package com.piranport.aviation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Client-side mirror of the player's locked fire control targets. */
@OnlyIn(Dist.CLIENT)
public class ClientFireControlData {

    private static List<UUID> lockedTargets = new ArrayList<>();

    public static void setTargets(List<UUID> targets) {
        lockedTargets = new ArrayList<>(targets);
    }

    public static List<UUID> getTargets() {
        return lockedTargets;
    }

    public static void clear() {
        lockedTargets.clear();
    }
}
