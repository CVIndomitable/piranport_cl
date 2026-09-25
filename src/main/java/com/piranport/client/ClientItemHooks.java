package com.piranport.client;

import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.client.input.ClientInputCoordinator;
import com.piranport.client.input.DebugInputHandler;
import com.piranport.client.input.EntityHighlightHandler;
import com.piranport.dungeon.client.DungeonContinueScreen;
import com.piranport.dungeon.client.DungeonHudLayer;
import com.piranport.dungeon.client.DungeonResultScreen;
import com.piranport.dungeon.client.DungeonReviveScreen;
import com.piranport.dungeon.client.TownScrollScreen;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.AircraftItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.network.CannonImpactEffectPayload;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class ClientItemHooks implements com.piranport.platform.ClientBridge {
    private static final int FRIENDLY_BLUE = 0x3399FF;
    private static final int HOSTILE_RED = 0xFF3333;
    private static final int FC_TARGET_RED = 0xFF0000;
    private static final int ALLY_GREEN = 0x33FF33;

    public ClientItemHooks() {}

    @Override
    public boolean isClient() { return true; }

    @Override
    public boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    @Override
    public Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public String getClientPlayerName() {
        var player = Minecraft.getInstance().player;
        return player != null ? player.getScoreboardName() : null;
    }

    @Override
    public long getClientGameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : -1L;
    }

    @Override
    public void resetClientState() {
        ClientInputCoordinator.resetClientState();
    }

    @Override
    public boolean isHighlightEnabled() {
        return EntityHighlightHandler.isHighlightEnabled();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void initializeSkinCoreItemClient(Object consumer) {
        if (consumer instanceof java.util.function.Consumer rawConsumer) {
            rawConsumer.accept(SkinCoreItemRenderer.CLIENT_EXTENSIONS);
        }
    }

    @Override
    public boolean toggleArtilleryScope(Player player, ItemStack stack) {
        if (!ClientScopeHandler.isScoping()) {
            ClientScopeHandler.enterScope(player, stack);
        }
        return true;
    }

    @Override
    public void handleTorpedoGuidanceState(boolean active, int entityId) {
        if (active) {
            ClientTorpedoGuidance.handleStart(entityId);
        } else {
            ClientTorpedoGuidance.handleEnd();
        }
    }

    @Override
    public void handleReconState(boolean active, int entityId) {
        if (active) {
            com.piranport.aviation.ClientReconData.handleReconStart(entityId);
        } else {
            com.piranport.aviation.ClientReconData.handleReconEnd();
        }
    }

    @Override
    public void triggerCameraShake(float intensity, int durationTicks) {
        CameraShakeHandler.trigger(intensity, durationTicks);
    }

    @Override
    public void triggerAircraftLaunchPose(int entityId, int skinId, int durationTicks) {
        AircraftLaunchPoseClientState.trigger(entityId, skinId, durationTicks);
    }

    @Override
    public void spawnCannonImpactEffect(CannonImpactEffectPayload payload) {
        CannonImpactEffects.spawn(payload);
    }

    @Override
    public void updateAswSonar(int aircraftEntityId, List<Integer> detectedEntityIds) {
        com.piranport.aviation.ClientAswSonarData.update(aircraftEntityId, detectedEntityIds);
    }

    @Override
    public void setFireControlTargets(List<UUID> targetUUIDs) {
        com.piranport.aviation.ClientFireControlData.setTargets(targetUUIDs);
    }

    @Override
    public void setFcRadarSnapLimit(double limitBlocks) {
        com.piranport.client.FireControlRadarSnapHandler.setServerSimulationLimitBlocks(limitBlocks);
    }

    @Override
    public void displayClientMessage(Component message) {
        displayClientMessage(message, true);
    }

    @Override
    public void displayClientMessage(Component message, boolean overlay) {
        Player player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(message, overlay);
    }

    @Override
    public void setTitle(Component title) {
        Minecraft.getInstance().gui.setTitle(title);
    }

    @Override
    public void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        Player player = Minecraft.getInstance().player;
        if (player != null) player.playSound(sound, volume, pitch);
    }

    @Override
    public void setDebugEnabledClient(boolean enabled) {
        DebugInputHandler.setDebugEnabledClient(enabled);
    }

    @Override
    public void setTestModeClient(boolean enabled) {
        DebugInputHandler.setTestModeClient(enabled);
    }

    @Override
    public void openDungeonContinueScreen(net.minecraft.core.BlockPos lecternPos, String stageName, int clearedNodeCount) {
        Minecraft.getInstance().setScreen(new DungeonContinueScreen(lecternPos, stageName, clearedNodeCount));
    }

    @Override
    public void setServerSolverStats(int ternaryIters, int newtonIters) {
        ClientScopeHandler.setServerSolverStats(ternaryIters, newtonIters);
    }

    @Override
    public void setServerSolverStats(int ternaryIters, int newtonIters, long totalUs,
                                            double verticalError, double horizontalError, double angleDeg) {
        ClientScopeHandler.setServerSolverStats(
                ternaryIters, newtonIters, totalUs, verticalError, horizontalError, angleDeg);
    }

    @Override
    public boolean isReconEntity(int entityId) {
        return com.piranport.aviation.ClientReconData.isInReconMode()
                && com.piranport.aviation.ClientReconData.getReconEntityId() == entityId;
    }

    @Override
    public boolean isInReconMode() {
        return com.piranport.aviation.ClientReconData.isInReconMode();
    }

    @Override
    public void openTownScrollScreen() {
        Minecraft.getInstance().setScreen(new TownScrollScreen());
    }

    @Override
    public void openDungeonResultScreen(String stageName, long timeMillis,
                                                boolean isFirstClear, List<String> rewardNames) {
        openDungeonResultScreen(stageName, timeMillis, isFirstClear, rewardNames, 0);
    }

    @Override
    public void openDungeonResultScreen(String stageName, long timeMillis,
                                        boolean isFirstClear, List<String> rewardNames, int kills) {
        DungeonHudLayer.clearDungeonState();
        Minecraft.getInstance().setScreen(new DungeonResultScreen(
                stageName, timeMillis, isFirstClear, rewardNames, kills));
    }

    @Override
    public void openDungeonReviveScreen() {
        Minecraft.getInstance().setScreen(new DungeonReviveScreen());
    }

    @Override
    public void updateDungeonNode(String nodeId) {
        DungeonHudLayer.updateNode(nodeId);
    }

    @Override
    public void setDungeonState(String stageName, String nodeId, long timerStartMillis) {
        DungeonHudLayer.setDungeonState(stageName, nodeId, timerStartMillis);
    }

    @Override
    public void updateDungeonBossOverlay(String bossName, String shipType, String chapter,
                                         int segment, float health, float maxHealth,
                                         boolean visible, boolean quietBattlefield) {
        DungeonHudLayer.updateBossOverlay(bossName, shipType, chapter, segment,
                health, maxHealth, visible, quietBattlefield);
    }

    @Override
    public boolean shouldAircraftGlow(AircraftEntity aircraft) {
        if (isFireControlTarget(aircraft)) {
            return true;
        }
        if (!EntityHighlightHandler.isHighlightEnabled()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return aircraft.getOwnerUUID() != null;
    }

    @Override
    public int getAircraftGlowColor(AircraftEntity aircraft, int fallbackColor) {
        if (isFireControlTarget(aircraft)) {
            return FC_TARGET_RED;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return fallbackColor;
        }
        if (aircraft.isOwnedByPlayer(mc.player)) {
            return FRIENDLY_BLUE;
        }
        java.util.UUID ownerUUID = aircraft.getOwnerUUID();
        if (ownerUUID != null && ownerUUID.equals(mc.player.getUUID())) {
            return FRIENDLY_BLUE;
        }
        if (aircraft.getFlightState() == AircraftEntity.FlightState.ATTACKING
                && (ownerUUID == null || !ownerUUID.equals(mc.player.getUUID()))) {
            return HOSTILE_RED;
        }
        return ALLY_GREEN;
    }

    private static boolean isFireControlTarget(AircraftEntity aircraft) {
        return com.piranport.aviation.ClientFireControlData.getTargets().contains(aircraft.getUUID());
    }

    /**
     * 数背包（含副手）里同口径鱼雷的总数。
     *
     * <p>刻意与 {@code TorpedoFireStrategy.fireTorpedosInventoryMode} 的扫描口径保持一致：
     * 只认口径、不限弹种，副手仅在武器不在副手时才计入。这里是客户端预测，
     * 只用来决定 tooltip 报"已装填"还是"未装填"，真正能不能打仍由服务端裁决。
     */
    private static int countInventoryTorpedoes(TorpedoLauncherItem launcher, Inventory inv) {
        int caliber = launcher.getCaliber();
        int available = 0;
        for (ItemStack s : inv.items) {
            if (!s.isEmpty() && s.getItem() instanceof com.piranport.item.TorpedoItem ti
                    && ti.getCaliber() == caliber) {
                available += s.getCount();
            }
        }
        ItemStack oh = inv.offhand.get(0);
        if (!oh.isEmpty() && oh.getItem() instanceof com.piranport.item.TorpedoItem ti
                && ti.getCaliber() == caliber) {
            available += oh.getCount();
        }
        return available;
    }

    /**
     * Appends current reload readiness for weapons that are directly in the
     * local player's inventory.
     */
    @Override
    public void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Inventory inv = mc.player.getInventory();
        ItemStack coreStack = TransformationManager.findTransformedCore(mc.player);
        if (coreStack.isEmpty()) return;

        int weaponSlot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i) == stack) {
                weaponSlot = i;
                break;
            }
        }
        if (weaponSlot == -1 && inv.offhand.get(0) == stack) weaponSlot = 40;
        if (weaponSlot == -1) return;

        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        long gameTime = mc.level.getGameTime();

        boolean isCannon = stack.getItem() instanceof com.piranport.artillery.ArtilleryItem;
        // 火炮计时跟随物品，换槽后不读取上一把武器留下的槽位镜像。
        boolean onCooldown = isCannon
                ? new com.piranport.combat.data.WeaponState(stack).isOnCooldown(gameTime)
                : cooldowns.isOnCooldown(weaponSlot, gameTime);
        boolean isManualMode = true;
        boolean isAutoReloadMissile = stack.getItem() instanceof MissileLauncherItem ml0 && !ml0.isManualReload();
        // 鱼雷发射器在任何模式下都可能"已装填 / 空膛 / 从背包现取"三态，必须单独判，
        // 不能像火炮那样一刀切（装填模式开时火炮走自动补给，鱼雷不走）。
        // WHY：否则装填模式开启（autoResupplyEnabled=true）时 needsLoadedAmmo 对鱼雷是 false，
        // 直接落到最后那个无条件 else 分支，无论膛里有没有鱼雷都报"已装填"——
        // 玩家看到的就是"显示装填了却打不出来"。
        boolean isTorpedoLauncher = stack.getItem() instanceof TorpedoLauncherItem;
        boolean needsLoadedAmmo = isTorpedoLauncher
                || (!isAutoReloadMissile
                && (isCannon
                || (isManualMode && !(stack.getItem() instanceof AircraftItem))
                || (stack.getItem() instanceof MissileLauncherItem ml && ml.isManualReload())));

        if (onCooldown) {
            if (isCannon) {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_reloading")
                        .withStyle(ChatFormatting.YELLOW));
            } else if (needsLoadedAmmo) {
                LoadedAmmo reloading = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
                tooltip.add(Component.translatable(reloading.hasAmmo()
                                ? "tooltip.piranport.weapon_reloading"
                                : "tooltip.piranport.weapon_not_loaded")
                        .withStyle(reloading.hasAmmo() ? ChatFormatting.YELLOW : ChatFormatting.RED));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_reloading")
                        .withStyle(ChatFormatting.YELLOW));
            }
        } else if (needsLoadedAmmo) {
            LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            boolean hasAmmo;
            if (stack.getItem() instanceof TorpedoLauncherItem tl) {
                // 两种弹药来源都算"能开火"：膛内（装填设施/右键装的）或背包里足量同口径鱼雷（再装填件自动取弹）。
                // WHY 保留 hasAmmo()：不可信来源（旧存档/组件编辑）可能留下 count>0 但 ammoItemId 为空的脏值，
                // 只看 count 会把这种管报成"已装填"。判定口径对齐 TorpedoFireStrategy 的手动模式分支。
                hasAmmo = (loaded.hasAmmo() && loaded.count() >= tl.getTubeCount())
                        || countInventoryTorpedoes(tl, inv) >= tl.getTubeCount();
            } else if (stack.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
                hasAmmo = loaded.count() >= ai.getEffectiveData(mc.level).barrels();
            } else {
                hasAmmo = loaded.hasAmmo();
            }
            tooltip.add(Component.translatable(hasAmmo
                            ? "tooltip.piranport.weapon_ready"
                            : "tooltip.piranport.weapon_not_loaded")
                    .withStyle(hasAmmo ? ChatFormatting.GREEN : ChatFormatting.RED));
        } else if (isAutoReloadMissile) {
            MissileLauncherItem mlCheck = (MissileLauncherItem) stack.getItem();
            Item ammoItem = mlCheck.getAmmoItem();
            boolean hasAmmoInInventory = false;
            for (ItemStack s : inv.items) {
                if (!s.isEmpty() && s.is(ammoItem)) {
                    hasAmmoInInventory = true;
                    break;
                }
            }
            if (!hasAmmoInInventory) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ammoItem)) hasAmmoInInventory = true;
            }
            tooltip.add(Component.translatable(hasAmmoInInventory
                            ? "tooltip.piranport.weapon_ready"
                            : "tooltip.piranport.weapon_not_loaded")
                    .withStyle(hasAmmoInInventory ? ChatFormatting.GREEN : ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.piranport.weapon_ready")
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
