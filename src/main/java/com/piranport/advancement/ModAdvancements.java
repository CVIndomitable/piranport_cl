package com.piranport.advancement;

import com.piranport.PiranPort;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Small helper for manually-awarded story advancements. */
public final class ModAdvancements {
    private static final String ROOT = "root";
    private static final String CRITERION = "done";

    private ModAdvancements() {
    }

    public static void award(ServerPlayer player, String path) {
        awardSingle(player, ROOT);
        awardSingle(player, path);
    }

    public static boolean has(ServerPlayer player, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, path);
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
        return advancement != null
                && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void awardSingle(ServerPlayer player, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, path);
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, CRITERION);
        }
    }
}
