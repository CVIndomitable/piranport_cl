package com.piranport.aviation;

import com.piranport.combat.TransformationManager;
import com.piranport.component.AircraftAttackMode;
import com.piranport.component.AircraftInfo;
import com.piranport.component.SlotCooldowns;
import com.piranport.config.ModCommonConfig;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.AircraftItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.AircraftLaunchPosePayload;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.skin.SkinManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * 飞机发射策略 — 从 {@link com.piranport.item.ShipCoreCombat} 提取的飞机相关静态方法集合。
 *
 * <p>负责舰载机的起飞、燃料补给、召回、自动升空以及起飞视觉效果：
 * <ul>
 *   <li>背包内起飞：消耗挂载物、播放起飞特效、清除物品、设置冷却</li>
 *   <li>起飞视觉效果：按皮肤 ID 选择起飞姿态、生成云雾/特征粒子、播放起飞音效</li>
 *   <li>燃料补给：消耗背包内航空燃料为舰载机加满油</li>
 *   <li>召回：召回所有存活的本舰载机</li>
 *   <li>自动升空：检测到幻影等敌对目标时自动起飞一架战斗机</li>
 * </ul>
 *
 * <p><b>线程模型</b>: 服务端主线程。
 * <p><b>包级访问</b>: 通过 {@code ShipCoreItem.SMALL_SHELLS} 访问标签字段（同包不可见，仅同包引用时不重要）。
 * <p><b>设计</b>: 纯静态方法集合，无实例状态。所有方法从 ShipCoreCombat 平移过来，保持方法体不变，仅将可见性从 private 调整为 public。
 *
 * @see com.piranport.item.ShipCoreCombat
 */
public class AircraftFireStrategy {

    private AircraftFireStrategy() {}

    // ===== Launch entrypoint =====

    public static void launchAircraftInventoryMode(Level level, Player player, ItemStack coreStack,
                                              Inventory inv, int weaponSlot, int coreInventorySlot,
                                              SlotCooldowns cooldowns) {
        ItemStack aircraftStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);

        // Fuel check — refuse launch if currentFuel == 0
        AircraftInfo launchInfo = aircraftStack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (launchInfo == null || launchInfo.currentFuel() <= 0) {
            player.displayClientMessage(Component.translatable("message.piranport.no_fuel"), true);
            return;
        }

        AircraftAttackMode attackMode = AircraftAttackMode.FOCUS;
        boolean hasBullets = launchInfo.aircraftType() == AircraftInfo.AircraftType.FIGHTER
                || launchInfo.aircraftType() == AircraftInfo.AircraftType.ROCKET_FIGHTER;
        String payloadType = "";

        switch (launchInfo.aircraftType()) {
            case TORPEDO_BOMBER -> { payloadType = "piranport:aerial_torpedo"; hasBullets = false; }
            case DIVE_BOMBER, LEVEL_BOMBER -> { payloadType = "piranport:aerial_bomb"; hasBullets = false; }
            case ASW -> { payloadType = "piranport:depth_charge"; hasBullets = false; }
            default -> { }
        }

        // Consume payload from inventory if needed
        if (!payloadType.isEmpty() && !hasBullets) {
            net.minecraft.world.item.Item payloadItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(payloadType));

            // 创造模式：即使没有挂载物也允许发射（使用默认挂载物）
            // 生存模式：消耗挂载物
            if (!player.getAbilities().instabuild) {
                // Survival mode: consume payload
                boolean consumed = false;
                for (int i = 0; i < inv.items.size(); i++) {
                    if (i == coreInventorySlot || i == weaponSlot) continue;
                    ItemStack s = inv.items.get(i);
                    if (!s.isEmpty() && s.getItem() == payloadItem) {
                        com.piranport.debug.PiranPortDebug.consumeAmmo(s, 1);
                        consumed = true;
                        break;
                    }
                }
                if (!consumed && weaponSlot != 40 && coreInventorySlot != 40) {
                    ItemStack oh = inv.offhand.get(0);
                    if (!oh.isEmpty() && oh.getItem() == payloadItem) {
                        com.piranport.debug.PiranPortDebug.consumeAmmo(oh, 1);
                        consumed = true;
                    }
                }
                if (!consumed) {
                    player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                    // P0-3: 起飞失败埋点（弹药不足）
                    com.piranport.debug.PiranPortDebug.aircraftLaunchFailed(
                            player, weaponSlot, aircraftStack, "NO_AMMO");
                    return;
                }
            }
        }

        AircraftEntity aircraft = AircraftEntity.create(level, player, weaponSlot, aircraftStack,
                attackMode, coreInventorySlot, hasBullets, payloadType);
        level.addFreshEntity(aircraft);
        spawnAircraftLaunchEffect(level, player, launchInfo.aircraftType());
        // P0-3: 起飞成功埋点（带玩家短UUID、槽位、物品hash、payload、mode、entityId）
        com.piranport.debug.PiranPortDebug.aircraftLaunched(
                player, weaponSlot, aircraftStack, payloadType, attackMode.name(), aircraft.getId());
        // 旧版事件保留，便于历史脚本兼容
        com.piranport.debug.PiranPortDebug.event(
                "Aircraft LAUNCH | type={} entityId={} payload={} mode={}",
                aircraft.getAircraftType().name(), aircraft.getId(), payloadType, attackMode.name());

        // Clear the aircraft from inventory
        if (weaponSlot == 40) {
            inv.offhand.set(0, ItemStack.EMPTY);
        } else {
            inv.items.set(weaponSlot, ItemStack.EMPTY);
        }

        int launchCooldown = TransformationManager.boostedCooldown(player, 20);
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cooldowns.withSlotCooldown(weaponSlot, launchCooldown, level.getGameTime()));


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.3f);
        player.displayClientMessage(
                Component.translatable("message.piranport.aircraft_launched", aircraftStack.getHoverName()), true);
    }

    public enum LaunchStyle {
        J_DECK_BOW,
        J_SNOW_BOW,
        J_CRUISER_BOW,
        J_RIBBON_BOW,
        C_SIGNAL,
        C_MISSILE_RAIL,
        G_MECHANICAL,
        G_SUBMARINE,
        I_CATAPULT,
        I_AERIAL_FRAME,
        E_LONGBOW,
        E_DECK_LONGBOW,
        U_MUSKET,
        U_CARRIER_CATAPULT,
        F_RAPIER,
        DEFAULT
    }

    public record LaunchProfile(LaunchStyle style, ParticleOptions accentParticle, SoundEvent sound,
                                 float volume, float pitch, double width, double lift, int accentBonus) {}

    public static void spawnAircraftLaunchEffect(Level level, Player player, AircraftInfo.AircraftType aircraftType) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0e-5) {
            horizontal = new Vec3(0, 0, 1);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 right = new Vec3(-horizontal.z, 0, horizontal.x);
        Vec3 origin = player.position().add(0, 1.1, 0).add(horizontal.scale(0.35));
        int skinId = SkinManager.getActiveSkin(player);
        LaunchProfile profile = resolveLaunchProfile(skinId);
        double poseRadius = serverLevel.getServer().getPlayerList().getSimulationDistance() * 16.0;
        PacketDistributor.sendToPlayersNear(
                serverLevel,
                null,
                player.getX(), player.getY(), player.getZ(),
                Math.max(48.0, poseRadius),
                new AircraftLaunchPosePayload(player.getId(), skinId, 18));

        for (int i = 0; i < 9; i++) {
            double t = (i - 4) / 4.0;
            Vec3 p = origin.add(right.scale(t * profile.width()))
                    .add(horizontal.scale(Math.abs(t) * 0.15))
                    .add(0, profile.lift(), 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z,
                    2, 0.04, 0.03, 0.04, 0.01);
        }

        spawnSkinLaunchGesture(serverLevel, origin, horizontal, right, profile, aircraftType);

        int accentCount = aircraftType == AircraftInfo.AircraftType.FIGHTER
                || aircraftType == AircraftInfo.AircraftType.ROCKET_FIGHTER ? 14 : 10;
        serverLevel.sendParticles(profile.accentParticle(),
                origin.x + horizontal.x * 0.6,
                origin.y + 0.1 + profile.lift(),
                origin.z + horizontal.z * 0.6,
                accentCount + profile.accentBonus(),
                0.35, 0.18, 0.35, 0.04);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                profile.sound(), SoundSource.PLAYERS, profile.volume(), profile.pitch());
    }

    public static LaunchProfile resolveLaunchProfile(int skinId) {
        return switch (skinId) {
            case 4 -> new LaunchProfile(LaunchStyle.J_DECK_BOW, ParticleTypes.CRIT,
                    SoundEvents.ARROW_SHOOT, 0.58f, 1.12f, 0.72, 0.04, 4);
            case 5 -> new LaunchProfile(LaunchStyle.C_MISSILE_RAIL, ParticleTypes.ENCHANT,
                    SoundEvents.CROSSBOW_SHOOT, 0.56f, 1.35f, 0.52, 0.02, 3);
            case 6 -> new LaunchProfile(LaunchStyle.C_SIGNAL, ParticleTypes.ENCHANT,
                    SoundEvents.AMETHYST_BLOCK_CHIME, 0.54f, 1.08f, 0.46, 0.02, 1);
            case 7 -> new LaunchProfile(LaunchStyle.C_SIGNAL, ParticleTypes.ENCHANT,
                    SoundEvents.AMETHYST_BLOCK_CHIME, 0.54f, 1.28f, 0.58, 0.05, 2);
            case 8 -> new LaunchProfile(LaunchStyle.J_SNOW_BOW, ParticleTypes.SNOWFLAKE,
                    SoundEvents.ARROW_SHOOT, 0.56f, 1.18f, 0.56, 0.03, 2);
            case 9 -> new LaunchProfile(LaunchStyle.J_SNOW_BOW, ParticleTypes.SNOWFLAKE,
                    SoundEvents.ARROW_SHOOT, 0.58f, 1.38f, 0.66, 0.06, 4);
            case 10 -> new LaunchProfile(LaunchStyle.I_CATAPULT, ParticleTypes.CRIT,
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.58f, 1.04f, 0.62, 0.02, 3);
            case 11 -> new LaunchProfile(LaunchStyle.J_CRUISER_BOW, ParticleTypes.CRIT,
                    SoundEvents.ARROW_SHOOT, 0.56f, 1.30f, 0.50, 0.04, 2);
            case 12 -> new LaunchProfile(LaunchStyle.G_MECHANICAL, ParticleTypes.WITCH,
                    SoundEvents.CROSSBOW_SHOOT, 0.56f, 0.95f, 0.54, 0.02, 2);
            case 13 -> new LaunchProfile(LaunchStyle.J_RIBBON_BOW, ParticleTypes.HEART,
                    SoundEvents.ARROW_SHOOT, 0.58f, 1.42f, 0.62, 0.07, 5);
            case 14 -> new LaunchProfile(LaunchStyle.C_SIGNAL, ParticleTypes.ENCHANT,
                    SoundEvents.AMETHYST_BLOCK_CHIME, 0.56f, 0.92f, 0.64, 0.04, 4);
            case 15 -> new LaunchProfile(LaunchStyle.C_MISSILE_RAIL, ParticleTypes.ENCHANT,
                    SoundEvents.CROSSBOW_SHOOT, 0.58f, 1.48f, 0.60, 0.04, 5);
            case 16 -> new LaunchProfile(LaunchStyle.I_AERIAL_FRAME, ParticleTypes.CRIT,
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.56f, 1.22f, 0.50, 0.03, 2);
            case 17 -> new LaunchProfile(LaunchStyle.G_SUBMARINE, ParticleTypes.BUBBLE,
                    SoundEvents.CROSSBOW_SHOOT, 0.48f, 0.78f, 0.42, -0.03, 1);
            case 18 -> new LaunchProfile(LaunchStyle.E_DECK_LONGBOW, ParticleTypes.END_ROD,
                    SoundEvents.ARROW_SHOOT, 0.56f, 1.06f, 0.68, 0.07, 4);
            case 19 -> new LaunchProfile(LaunchStyle.F_RAPIER, ParticleTypes.CRIT,
                    SoundEvents.CROSSBOW_SHOOT, 0.54f, 1.26f, 0.48, 0.04, 3);
            case 20 -> new LaunchProfile(LaunchStyle.U_CARRIER_CATAPULT, ParticleTypes.END_ROD,
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.62f, 0.98f, 0.76, 0.02, 5);
            case 21 -> new LaunchProfile(LaunchStyle.U_MUSKET, ParticleTypes.POOF,
                    SoundEvents.FIREWORK_ROCKET_BLAST, 0.58f, 1.16f, 0.50, 0.02, 3);
            case 22 -> new LaunchProfile(LaunchStyle.E_LONGBOW, ParticleTypes.END_ROD,
                    SoundEvents.ARROW_SHOOT, 0.56f, 1.34f, 0.56, 0.08, 3);
            case 23 -> new LaunchProfile(LaunchStyle.E_LONGBOW, ParticleTypes.END_ROD,
                    SoundEvents.ARROW_SHOOT, 0.58f, 1.30f, 0.58, 0.08, 3);
            default -> new LaunchProfile(LaunchStyle.DEFAULT, ParticleTypes.FIREWORK,
                    SoundEvents.CROSSBOW_SHOOT, 0.55f, 1.25f, 0.55, 0.0, 0);
        };
    }

    public static void spawnSkinLaunchGesture(ServerLevel level, Vec3 origin, Vec3 forward, Vec3 right,
                                               LaunchProfile profile, AircraftInfo.AircraftType aircraftType) {
        boolean fighter = aircraftType == AircraftInfo.AircraftType.FIGHTER
                || aircraftType == AircraftInfo.AircraftType.ROCKET_FIGHTER;
        switch (profile.style()) {
            case J_DECK_BOW -> {
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.CRIT, 7, 0.34, 0.02);
                spawnBowArc(level, origin.add(0, 0.08, 0), forward, right, ParticleTypes.CRIT, 0.58, 0.24);
                Vec3 arrow = origin.add(forward.scale(fighter ? 1.05 : 0.82)).add(0, 0.24, 0);
                level.sendParticles(ParticleTypes.END_ROD, arrow.x, arrow.y, arrow.z,
                        fighter ? 12 : 8, 0.08, 0.05, 0.08, 0.02);
            }
            case J_SNOW_BOW -> {
                spawnBowArc(level, origin, forward, right, ParticleTypes.SNOWFLAKE, 0.52, 0.22);
                for (int i = 0; i < 8; i++) {
                    Vec3 p = origin.add(forward.scale(0.16 * i)).add(0, 0.08 + i * 0.018, 0);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z,
                            2, 0.04, 0.04, 0.04, 0.0);
                }
            }
            case J_CRUISER_BOW -> {
                spawnBowArc(level, origin.add(0, 0.05, 0), forward, right, ParticleTypes.CRIT, 0.44, 0.20);
                for (int i = -1; i <= 1; i++) {
                    Vec3 p = origin.add(right.scale(i * 0.18)).add(forward.scale(0.22)).add(0, 0.12, 0);
                    level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z,
                            fighter ? 4 : 3, 0.02, 0.02, 0.02, 0.01);
                }
            }
            case J_RIBBON_BOW -> {
                spawnBowArc(level, origin.add(0, 0.06, 0), forward, right, ParticleTypes.CRIT, 0.56, 0.20);
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI * 2.0 / 6.0;
                    Vec3 p = origin.add(right.scale(Math.cos(angle) * 0.32))
                            .add(forward.scale(Math.sin(angle) * 0.12))
                            .add(0, 0.18, 0);
                    level.sendParticles(ParticleTypes.HEART, p.x, p.y, p.z,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            case C_SIGNAL -> {
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI * 2.0 / 8.0;
                    Vec3 p = origin.add(right.scale(Math.cos(angle) * profile.width() * 0.75))
                            .add(forward.scale(Math.sin(angle) * 0.20))
                            .add(0, 0.12 + profile.lift(), 0);
                    level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z,
                            2, 0.02, 0.02, 0.02, 0.0);
                }
                if (profile.accentBonus() >= 4) {
                    spawnSignalBars(level, origin, right, ParticleTypes.ENCHANT);
                }
            }
            case C_MISSILE_RAIL -> {
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.ENCHANT, 9, 0.22, profile.lift());
                for (int side = -1; side <= 1; side += 2) {
                    Vec3 rail = origin.add(right.scale(side * profile.width() * 0.55)).add(forward.scale(0.18));
                    level.sendParticles(ParticleTypes.ENCHANT, rail.x, rail.y + 0.1, rail.z,
                            fighter ? 7 : 5, 0.03, 0.04, 0.03, 0.02);
                }
            }
            case G_MECHANICAL -> {
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI * 2.0 / 6.0;
                    Vec3 p = origin.add(right.scale(Math.cos(angle) * 0.34))
                            .add(forward.scale(Math.sin(angle) * 0.16))
                            .add(0, 0.10, 0);
                    level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z,
                            2, 0.02, 0.02, 0.02, 0.01);
                }
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.CRIT, 5, 0.18, 0.03);
            }
            case G_SUBMARINE -> {
                for (int i = 0; i < 9; i++) {
                    Vec3 p = origin.add(forward.scale(i * 0.10)).add(0, -0.08 + i * 0.012, 0)
                            .add(right.scale(Math.sin(i * 0.8) * 0.10));
                    level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z,
                            3, 0.03, 0.02, 0.03, 0.01);
                }
                level.sendParticles(ParticleTypes.WITCH,
                        origin.x, origin.y + 0.05, origin.z,
                        fighter ? 8 : 5, 0.16, 0.05, 0.16, 0.01);
            }
            case I_CATAPULT -> {
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.CRIT, 9, 0.30, 0.02);
                Vec3 exhaust = origin.add(forward.scale(-0.10)).add(0, 0.05, 0);
                level.sendParticles(ParticleTypes.POOF, exhaust.x, exhaust.y, exhaust.z,
                        fighter ? 10 : 7, 0.16, 0.05, 0.16, 0.02);
            }
            case I_AERIAL_FRAME -> {
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < 4; i++) {
                        Vec3 p = origin.add(right.scale(side * (0.18 + i * 0.06)))
                                .add(forward.scale(0.10 + i * 0.05))
                                .add(0, 0.08 + i * 0.018, 0);
                        level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z,
                                2, 0.02, 0.02, 0.02, 0.01);
                    }
                }
            }
            case E_LONGBOW, E_DECK_LONGBOW -> {
                double height = profile.style() == LaunchStyle.E_DECK_LONGBOW ? 0.52 : 0.44;
                for (int i = 0; i < 13; i++) {
                    double t = (i - 6) / 6.0;
                    Vec3 p = origin.add(right.scale(t * 0.30))
                            .add(forward.scale(Math.abs(t) * 0.10))
                            .add(0, height - Math.abs(t) * 0.34, 0);
                    level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z,
                            1, 0.01, 0.01, 0.01, 0.0);
                }
                if (profile.style() == LaunchStyle.E_DECK_LONGBOW) {
                    spawnRunwayStreak(level, origin, forward, right, ParticleTypes.END_ROD, 7, 0.30, 0.04);
                }
                Vec3 arrow = origin.add(forward.scale(fighter ? 1.05 : 0.82)).add(0, height * 0.75, 0);
                level.sendParticles(ParticleTypes.END_ROD, arrow.x, arrow.y, arrow.z,
                        fighter ? 12 : 8, 0.08, 0.05, 0.08, 0.02);
            }
            case U_MUSKET -> {
                Vec3 muzzle = origin.add(forward.scale(0.64)).add(0, 0.12, 0);
                level.sendParticles(ParticleTypes.POOF, muzzle.x, muzzle.y, muzzle.z,
                        fighter ? 12 : 8, 0.12, 0.04, 0.12, 0.02);
                level.sendParticles(ParticleTypes.CRIT, muzzle.x + forward.x * 0.15, muzzle.y, muzzle.z + forward.z * 0.15,
                        fighter ? 9 : 6, 0.04, 0.02, 0.04, 0.04);
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.POOF, 5, 0.16, 0.00);
            }
            case U_CARRIER_CATAPULT -> {
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.END_ROD, 11, 0.36, 0.02);
                Vec3 exhaust = origin.add(forward.scale(-0.12));
                level.sendParticles(ParticleTypes.POOF, exhaust.x, exhaust.y, exhaust.z,
                        fighter ? 12 : 8, 0.20, 0.06, 0.20, 0.025);
            }
            case F_RAPIER -> {
                for (int i = 0; i < 8; i++) {
                    Vec3 p = origin.add(forward.scale(i * 0.13))
                            .add(right.scale((i - 3.5) * 0.035))
                            .add(0, 0.12 + i * 0.012, 0);
                    level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z,
                            2, 0.02, 0.02, 0.02, 0.02);
                }
                Vec3 flourish = origin.add(forward.scale(0.34)).add(right.scale(0.22)).add(0, 0.22, 0);
                level.sendParticles(ParticleTypes.ENCHANT, flourish.x, flourish.y, flourish.z,
                        7, 0.06, 0.05, 0.06, 0.01);
            }
            case DEFAULT -> {
                Vec3 p = origin.add(forward.scale(0.45)).add(0, 0.12, 0);
                level.sendParticles(ParticleTypes.FIREWORK, p.x, p.y, p.z,
                        fighter ? 8 : 5, 0.18, 0.08, 0.18, 0.03);
            }
        }
    }

    public static void spawnBowArc(ServerLevel level, Vec3 origin, Vec3 forward, Vec3 right,
                                    ParticleOptions particle, double width, double height) {
        for (int i = 0; i < 13; i++) {
            double t = (i - 6) / 6.0;
            Vec3 p = origin.add(right.scale(t * width))
                    .add(forward.scale(Math.abs(t) * 0.08))
                    .add(0, height - Math.abs(t) * height * 0.72, 0);
            level.sendParticles(particle, p.x, p.y, p.z,
                    1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    public static void spawnRunwayStreak(ServerLevel level, Vec3 origin, Vec3 forward, Vec3 right,
                                          ParticleOptions particle, int points, double halfWidth, double lift) {
        for (int i = 0; i < points; i++) {
            double t = points <= 1 ? 0.0 : i / (double) (points - 1);
            double side = i % 2 == 0 ? -halfWidth : halfWidth;
            Vec3 p = origin.add(forward.scale(t * 0.95))
                    .add(right.scale(side * (0.28 + 0.72 * t)))
                    .add(0, 0.04 + lift + t * 0.10, 0);
            level.sendParticles(particle, p.x, p.y, p.z,
                    2, 0.02, 0.02, 0.02, 0.01);
        }
    }

    public static void spawnSignalBars(ServerLevel level, Vec3 origin, Vec3 right, ParticleOptions particle) {
        for (int bar = 0; bar < 3; bar++) {
            for (int i = -2; i <= 2; i++) {
                Vec3 p = origin.add(right.scale(i * 0.08)).add(0, 0.10 + bar * 0.08, 0);
                level.sendParticles(particle, p.x, p.y, p.z,
                        1, 0.01, 0.01, 0.01, 0.0);
            }
        }
    }

    // ===== Fuel refill =====

    /**
     * Recall all airborne aircraft owned by this player. Returns the count recalled.
     */
    public static int recallAllAircraft(ServerLevel level, Player player) {
        UUID ownerUUID = player.getUUID();
        // Reduced recall range from 300 to 128 blocks for better performance
        List<AircraftEntity> aircraft = level.getEntitiesOfClass(
                AircraftEntity.class,
                new AABB(player.getX() - 128, player.getY() - 64, player.getZ() - 128,
                         player.getX() + 128, player.getY() + 64, player.getZ() + 128),
                a -> ownerUUID.equals(a.getOwnerUUID()) && a.isAlive());
        for (AircraftEntity a : aircraft) {
            a.startReturning("core_recall");
        }
        // End recon mode if active
        if (!aircraft.isEmpty()) {
            ReconManager.endRecon(ownerUUID);
            FireControlManager.clearTargets(ownerUUID);
        }
        return aircraft.size();
    }

    /**
     * On transformation: consume aviation_fuel to fill aircraft.
     * One aviation_fuel item fills one aircraft to full fuelCapacity.
     */
    public static void refillAircraftFuel(Player player, ItemStack coreStack) {
        if (!ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) return; // manual mode: no auto fuel
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return;

        refillAircraftFuelInventoryMode(player);
    }

    /**
     * Inventory mode fuel refill: scan inventory for AircraftItem stacks and aviation_fuel,
     * consuming one fuel per aircraft that needs it.
     */
    public static void refillAircraftFuelInventoryMode(Player player) {
        Inventory inv = player.getInventory();

        for (ItemStack weapon : inv.items) {
            if (!(weapon.getItem() instanceof AircraftItem)) continue;
            AircraftInfo info = weapon.get(ModDataComponents.AIRCRAFT_INFO.get());
            if (info == null || info.currentFuel() >= info.fuelCapacity()) {
                continue;
            }
            // Find aviation_fuel in inventory
            for (ItemStack ammo : inv.items) {
                if (ammo.is(ModItems.AVIATION_FUEL.get()) && ammo.getCount() > 0) {
                    com.piranport.debug.PiranPortDebug.consumeAmmo(ammo, 1);
                    weapon.set(ModDataComponents.AIRCRAFT_INFO.get(),
                            info.withCurrentFuel(info.fuelCapacity()));
                    break;
                }
            }
        }
    }

    // ===== Auto-launch (Phase 36) =====

    /**
     * Server-side: refuel + launch the first available FIGHTER in weapon slots.
     * Called by the auto-launch tick when phantoms are detected nearby.
     * Returns true if a fighter was launched.
     */
    public static boolean tryAutoLaunchFighter(Level level, Player player, ItemStack coreStack, int coreSlot) {
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return false;
        if (level.isClientSide()) return false;

        // Refuel aircraft before checking.
        refillAircraftFuel(player, coreStack);

        Inventory inv = player.getInventory();
        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        long gameTime = level.getGameTime();

        for (int wi = 0; wi < 9; wi++) {
            if (wi == coreSlot) continue;
            if (cooldowns.isOnCooldown(wi, gameTime)) continue;

            ItemStack weapon = inv.items.get(wi);
            if (weapon.isEmpty() || !(weapon.getItem() instanceof AircraftItem)) continue;
            AircraftInfo info = weapon.get(ModDataComponents.AIRCRAFT_INFO.get());
            if (info == null || info.currentFuel() <= 0) continue;

            boolean isFighter = info.aircraftType() == AircraftInfo.AircraftType.FIGHTER
                    || info.aircraftType() == AircraftInfo.AircraftType.ROCKET_FIGHTER;
            if (!isFighter) continue;

            launchAircraftInventoryMode(level, player, coreStack, inv, wi, coreSlot, cooldowns);
            com.piranport.debug.PiranPortDebug.event(
                    "Auto fighter launch | slot={} aircraft={}",
                    wi, BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath());
            return true;
        }
        return false;
    }
}
