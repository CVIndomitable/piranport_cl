package com.piranport.platform;

import com.piranport.entity.AircraftEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Common-side bridge for client-only helpers.
 *
 * <p>Do not reference Minecraft client classes from this class. It may be loaded
 * on dedicated servers through shared item/entity classes.
 */
public final class ClientHooks {
    private static final Class<?> CLIENT_HOOKS = loadClientHooks();

    private ClientHooks() {}

    public static boolean isClient() {
        return CLIENT_HOOKS != null;
    }

    public static boolean hasShiftDown() {
        Object result = invoke("hasShiftDown");
        return result instanceof Boolean value && value;
    }

    public static Player getClientPlayer() {
        Object result = invoke("getClientPlayer");
        return result instanceof Player player ? player : null;
    }

    public static long getClientGameTime() {
        Object result = invoke("getClientGameTime");
        return result instanceof Long value ? value : -1L;
    }

    public static boolean toggleArtilleryScope(Player player, ItemStack stack) {
        Object result = invoke("toggleArtilleryScope",
                new Class<?>[] { Player.class, ItemStack.class }, player, stack);
        return result instanceof Boolean value && value;
    }

    public static void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip) {
        invoke("appendWeaponCooldownTooltip",
                new Class<?>[] { ItemStack.class, List.class }, stack, tooltip);
    }

    public static void handleTorpedoGuidanceState(boolean active, int entityId) {
        invoke("handleTorpedoGuidanceState",
                new Class<?>[] { boolean.class, int.class }, active, entityId);
    }

    public static void handleReconState(boolean active, int entityId) {
        invoke("handleReconState",
                new Class<?>[] { boolean.class, int.class }, active, entityId);
    }

    public static void triggerCameraShake(float intensity, int durationTicks) {
        invoke("triggerCameraShake",
                new Class<?>[] { float.class, int.class }, intensity, durationTicks);
    }

    public static void spawnCannonImpactEffect(Object payload) {
        invoke("spawnCannonImpactEffect", new Class<?>[] { Object.class }, payload);
    }

    public static void updateAswSonar(int aircraftEntityId, List<Integer> detectedEntityIds) {
        invoke("updateAswSonar",
                new Class<?>[] { int.class, List.class }, aircraftEntityId, detectedEntityIds);
    }

    public static void setFireControlTargets(List<UUID> targetUUIDs) {
        invoke("setFireControlTargets", new Class<?>[] { List.class }, targetUUIDs);
    }

    public static boolean isReconEntity(int entityId) {
        Object result = invoke("isReconEntity",
                new Class<?>[] { int.class }, entityId);
        return result instanceof Boolean value && value;
    }

    public static boolean isInReconMode() {
        Object result = invoke("isInReconMode");
        return result instanceof Boolean value && value;
    }

    public static void openTownScrollScreen() {
        invoke("openTownScrollScreen");
    }

    public static void openDungeonResultScreen(String stageName, long timeMillis,
                                                boolean isFirstClear, List<String> rewardNames) {
        invoke("openDungeonResultScreen",
                new Class<?>[] { String.class, long.class, boolean.class, List.class },
                stageName, timeMillis, isFirstClear, rewardNames);
    }

    public static void openDungeonReviveScreen() {
        invoke("openDungeonReviveScreen");
    }

    public static void updateDungeonNode(String nodeId) {
        invoke("updateDungeonNode", new Class<?>[] { String.class }, nodeId);
    }

    public static void setDungeonState(String stageName, String nodeId, long timerStartMillis) {
        invoke("setDungeonState",
                new Class<?>[] { String.class, String.class, long.class },
                stageName, nodeId, timerStartMillis);
    }

    public static boolean shouldAircraftGlow(AircraftEntity aircraft) {
        Object result = invoke("shouldAircraftGlow",
                new Class<?>[] { AircraftEntity.class }, aircraft);
        return result instanceof Boolean value && value;
    }

    public static int getAircraftGlowColor(AircraftEntity aircraft, int fallbackColor) {
        Object result = invoke("getAircraftGlowColor",
                new Class<?>[] { AircraftEntity.class, int.class }, aircraft, fallbackColor);
        return result instanceof Integer value ? value : fallbackColor;
    }

    private static Class<?> loadClientHooks() {
        if (!FMLEnvironment.dist.isClient()) {
            return null;
        }
        try {
            return Class.forName("com.piranport.client.ClientItemHooks");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Object invoke(String methodName) {
        return invoke(methodName, new Class<?>[0]);
    }

    private static Object invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (CLIENT_HOOKS == null) {
            return null;
        }
        try {
            Method method = CLIENT_HOOKS.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
