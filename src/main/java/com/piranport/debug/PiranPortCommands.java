package com.piranport.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.FloatingTargetEntity;
import com.piranport.entity.MissileEntity;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.worldgen.LootChestProcessor;
import com.piranport.worldgen.RuinDegradationProcessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;
import java.util.Optional;

/**
 * Debug commands for PiranPort mod.
 * Registered under /ppd (Piran Port Debug).
 */
public final class PiranPortCommands {

    private PiranPortCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ppd")
                .requires(src -> src.hasPermission(2))

                // /ppd spawn_ruin <type>
                .then(Commands.literal("spawn_ruin")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("portal_ruin");
                                    builder.suggest("supply_depot");
                                    builder.suggest("outpost");
                                    builder.suggest("abyssal_base");
                                    builder.suggest("abandoned_portal");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> spawnRuin(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))

                // /ppd spawn_abyssal <entity_type> [count]
                .then(Commands.literal("spawn_abyssal")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("deep_ocean_supply");
                                    builder.suggest("deep_ocean_destroyer");
                                    builder.suggest("deep_ocean_light_cruiser");
                                    builder.suggest("deep_ocean_heavy_cruiser");
                                    builder.suggest("deep_ocean_battle_cruiser");
                                    builder.suggest("deep_ocean_battleship");
                                    builder.suggest("deep_ocean_light_carrier");
                                    builder.suggest("deep_ocean_carrier");
                                    builder.suggest("deep_ocean_submarine");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> spawnAbyssal(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "entity_type"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 20))
                                        .executes(ctx -> spawnAbyssal(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "entity_type"),
                                                IntegerArgumentType.getInteger(ctx, "count"))))))

                // /ppd spawn_ship_girl <variant>
                .then(Commands.literal("spawn_ship_girl")
                        .then(Commands.argument("variant", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("random");
                                    builder.suggest("fubuki");
                                    builder.suggest("fubuki_g");
                                    builder.suggest("unicorn");
                                    builder.suggest("kitchen_goddess");
                                    builder.suggest("hood");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> spawnShipGirl(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "variant")))))

                // /ppd target_fire
                .then(Commands.literal("target_fire")
                        .executes(ctx -> targetFire(ctx.getSource())))

                // /ppd target_b25
                .then(Commands.literal("target_b25")
                        .executes(ctx -> targetB25(ctx.getSource())))

                // /ppd model_debug <model> [variant] [force]
                // variant 只对弹体有意义：shell 取 small/medium/large（口径），
                // missile 取 anti_air/anti_ship/rocket（弹种）。省略时取各族默认档。
                // force 用于确认在非本指令放置的区域内施工（见 modelDebug 的破坏性防护）。
                .then(Commands.literal("model_debug")
                        .then(Commands.argument("model", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("b25");
                                    builder.suggest("f4f");
                                    builder.suggest("heavy_cruiser");
                                    builder.suggest("light_carrier");
                                    builder.suggest("torpedo");
                                    builder.suggest("shell");
                                    builder.suggest("missile");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> modelDebug(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "model"), "", false))
                                .then(Commands.argument("variant", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("small");
                                            builder.suggest("medium");
                                            builder.suggest("large");
                                            builder.suggest("anti_air");
                                            builder.suggest("anti_ship");
                                            builder.suggest("rocket");
                                            builder.suggest("force");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> modelDebug(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "model"),
                                                StringArgumentType.getString(ctx, "variant"), false))
                                        .then(Commands.argument("force", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("force");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> modelDebug(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "model"),
                                                        StringArgumentType.getString(ctx, "variant"),
                                                        true))))))

                // /ppd acceptance_kit <type>
                .then(Commands.literal("acceptance_kit")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("blueprint_chest");
                                    builder.suggest("stove");
                                    builder.suggest("artillery");
                                    builder.suggest("deep_ocean");
                                    builder.suggest("ship_girl");
                                    builder.suggest("trees");
                                    builder.suggest("all");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> acceptanceKit(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))

                // /ppd locate_ruin <type>
                .then(Commands.literal("locate_ruin")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("portal_ruin");
                                    builder.suggest("supply_depot");
                                    builder.suggest("outpost");
                                    builder.suggest("abyssal_base");
                                    builder.suggest("abandoned_portal");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> locateRuin(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))

                // P2-10: TPS / 调试开销监控 — 对比调试开启 / 关闭时的差异
                .then(Commands.literal("tps")
                        .executes(ctx -> tpsReport(ctx.getSource())))
        );
    }

    /**
     * 输出当前服务端 TPS、平均 tick 耗时、调试会话数。
     * 配合 F8 开关的多次采样，可以估算调试系统对主线程的影响。
     */
    private static int tpsReport(CommandSourceStack source) {
        var server = source.getServer();
        long tickCount = server.getTickCount();
        long nanoTime = System.nanoTime();

        // 用最近 N 秒的 tick 数估算 TPS（每次调用独立计算，简单且无副作用）
        // 注意：这是粗略估算，MC 服务端 tick 时长应为 50ms（20 TPS）
        long recentTicks;
        long recentNanos;
        synchronized (TPS_SAMPLE_LOCK) {
            // tickCount 会随退出存档/新建存档归零，而这两个静态字段跨存档保留，
            // 归档后会得到"当前 tick 数 < 上次采样 tick 数"的负差，tps 直接变成负数/垃圾值。
            // 必须把"采样 tick 比当前 tick 还新"当作失效基线重新采样。
            if (tpsSampleTick == -1 || tickCount < tpsSampleTick
                    || nanoTime - tpsSampleNanos > TPS_SAMPLE_INTERVAL_NS) {
                tpsSampleTick = tickCount;
                tpsSampleNanos = nanoTime;
            }
            recentTicks = tickCount - tpsSampleTick;
            recentNanos = nanoTime - tpsSampleNanos;
        }
        double tps = recentNanos > 0 ? (double) recentTicks * 1_000_000_000.0 / recentNanos : 0.0;
        double avgTickMs = recentTicks > 0 ? (double) recentNanos / recentTicks / 1_000_000.0 : 0.0;

        int sessionCount = com.piranport.debug.PiranPortDebug.isServerEnabled() ? 1 : 0;
        boolean testMode = com.piranport.testtools.PiranPortTestTools.isTestModeActive();

        String msg = String.format(java.util.Locale.ROOT,
                "§7[PP TPS] tick=%d tps=%.2f avgTick=%.2fms debugSession=%s testMode=%s",
                tickCount, tps, avgTickMs,
                sessionCount > 0 ? "ACTIVE" : "off",
                testMode ? "§cON" : "off");
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    // /ppd model_debug 的合法取值。必须与 ModelDebugBlockEntityRenderer 的两个 switch 保持一致，
    // 校验与渲染读同一份名单，避免再次出现"补了渲染忘了补校验"的漏档。
    private static final java.util.List<String> MODEL_DEBUG_TYPES = java.util.List.of(
            "b25", "f4f", "heavy_cruiser", "light_carrier", "torpedo", "shell", "missile");
    private static final java.util.List<String> MODEL_DEBUG_VARIANTS = java.util.List.of(
            "small", "medium", "large", "anti_air", "anti_ship", "rocket");

    /** 弹体域：只有 shell / missile 吃 variant，其余模型忽略该字段。 */
    private static final java.util.List<String> SHELL_VARIANTS = java.util.List.of("small", "medium", "large");
    private static final java.util.List<String> MISSILE_VARIANTS = java.util.List.of("anti_air", "anti_ship", "rocket");

    /**
     * 型号是否支持某个变体。
     *
     * <p>两张独立白名单交叉出四种组合，只有两种合法；不查交叉会让
     * {@code /ppd model_debug shell rocket} 通过校验然后落进渲染器 default 分支。
     */
    private static boolean isValidModelVariant(String modelType, String variant) {
        return switch (modelType) {
            case "shell" -> SHELL_VARIANTS.contains(variant);
            case "missile" -> MISSILE_VARIANTS.contains(variant);
            // 非弹体模型：variant 会被忽略，任何空串以外的值都算"填了没用的参数"，
            // 直接按不支持处理，免得玩家以为换了贴图。
            default -> false;
        };
    }

    private static java.util.List<String> variantsForModel(String modelType) {
        return switch (modelType) {
            case "shell" -> SHELL_VARIANTS;
            case "missile" -> MISSILE_VARIANTS;
            default -> java.util.List.of();
        };
    }

    private static final Object TPS_SAMPLE_LOCK = new Object();
    private static long tpsSampleTick = -1L;
    private static long tpsSampleNanos = -1L;
    private static final long TPS_SAMPLE_INTERVAL_NS = 5_000_000_000L; // 每 5 秒重采样

    private static int spawnShipGirl(CommandSourceStack source, String variant) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        int skinId = switch (variant) {
            case "random" -> 0;
            case "fubuki" -> 8;
            case "fubuki_g" -> 9;
            case "unicorn" -> 18;
            case "kitchen_goddess" -> ShipGirlEntity.KITCHEN_GODDESS_VARIANT;
            case "hood" -> 23;
            default -> {
                source.sendFailure(Component.literal("Unknown ship girl variant: " + variant
                        + ". Use: random, fubuki, fubuki_g, unicorn, kitchen_goddess, hood"));
                yield -1;
            }
        };
        if (skinId < 0) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        ShipGirlEntity shipGirl = ModEntityTypes.SHIP_GIRL.get().create(level);
        if (shipGirl == null) {
            source.sendFailure(Component.literal("Failed to create ship girl"));
            return 0;
        }
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
        shipGirl.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                player.getYRot() + 180.0F, 0.0F);
        if (skinId > 0) {
            shipGirl.setSkinVariant(skinId);
        }
        if (!level.noCollision(shipGirl)) {
            source.sendFailure(Component.literal("No room to spawn ship girl"));
            return 0;
        }
        level.addFreshEntity(shipGirl);
        source.sendSuccess(() -> Component.literal("Spawned ship girl variant " + variant), false);
        return 1;
    }

    private static int spawnRuin(CommandSourceStack source, String type) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        int variantCount = switch (type) {
            case "portal_ruin", "supply_depot", "outpost", "abyssal_base", "abandoned_portal" -> 2;
            default -> {
                source.sendFailure(Component.literal("Unknown ruin type: " + type
                        + ". Use: portal_ruin, supply_depot, outpost, abyssal_base, abandoned_portal"));
                yield 0;
            }
        };
        if (variantCount <= 0) return 0;

        int variant = 1 + level.random.nextInt(variantCount);
        ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath("piranport", type + "_" + variant);

        Optional<StructureTemplate> template = level.getStructureManager().get(templateId);
        if (template.isEmpty()) {
            source.sendFailure(Component.literal("Missing structure template: " + templateId));
            return 0;
        }

        Vec3i size = template.get().getSize();
        BlockPos origin = player.blockPosition().offset(-size.getX() / 2, -1, -size.getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false)
                .addProcessor(new LootChestProcessor(lootTableForRuin(type)))
                .addProcessor(new RuinDegradationProcessor(integrityForRuin(type)));
        boolean placed = template.get().placeInWorld(level, origin, origin, settings, level.random, 3);
        if (!placed) {
            source.sendFailure(Component.literal("Failed to place structure template: " + templateId));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Placed " + templateId
                + " at " + origin.toShortString() + " size " + size.getX() + "x" + size.getY() + "x" + size.getZ()), true);
        return 1;
    }

    private static ResourceLocation lootTableForRuin(String type) {
        return ResourceLocation.fromNamespaceAndPath("piranport", "chests/" + type);
    }

    private static float integrityForRuin(String type) {
        return switch (type) {
            case "portal_ruin" -> 0.85f;
            case "supply_depot" -> 0.80f;
            case "outpost" -> 0.75f;
            case "abyssal_base" -> 0.70f;
            case "abandoned_portal" -> 1.0f;
            default -> 1.0f;
        };
    }

    private static int spawnAbyssal(CommandSourceStack source, String entityId, int count) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("piranport", entityId);
        Optional<EntityType<?>> optType = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
        if (optType.isEmpty()) {
            source.sendFailure(Component.literal("Unknown entity type: piranport:" + entityId));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        EntityType<?> type = optType.get();
        java.util.UUID cluster = java.util.UUID.randomUUID();
        com.piranport.npc.ai.FleetGroupManager mgr = com.piranport.npc.ai.FleetGroupManager.get(level);
        mgr.createGroup(cluster);

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Entity entity = type.create(level);
            if (entity == null) continue;

            // 类型白名单必须在 create() 之后判：EntityType.getBaseClass() 在 1.21.1 恒返回
            // Entity.class（见 EntityType 源码，就是个裸 return），拿它做 isAssignableFrom
            // 既挡不住飞机/导弹，反过来写错方向还会把 9 种合法深海舰全拒掉。
            // 唯一可靠的办法是看造出来的实体本身。
            if (!(entity instanceof AbstractDeepOceanEntity abyssal)) {
                source.sendFailure(Component.literal(
                        "§c该实体不是深海敌舰，禁止用此指令生成: piranport:" + entityId));
                // 已建的空编组留给 FleetGroupManager 惰性清理
                return 0;
            }

            double offsetX = (level.random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 6.0;
            entity.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);

            EventHooks.finalizeMobSpawn(abyssal, level, level.getCurrentDifficultyAt(player.blockPosition()),
                    MobSpawnType.COMMAND, null);

            abyssal.setFleetGroupId(cluster);
            mgr.addMember(cluster, abyssal.getUUID());

            level.addFreshEntity(entity);
            spawned++;
        }

        // 一个都没造出来时必须回收刚建的空编组：create() 返回 null 或实体被拒时
        // 留下的是永久空组（FleetGroupManager 只提供惰性 cleanup，不保证这一帧会跑到）。
        if (spawned == 0) {
            mgr.cleanup(player.getServer());
            source.sendFailure(Component.literal("§c未能生成任何实体（实体创建被拒绝）"));
            return 0;
        }

        int finalSpawned = spawned;
        source.sendSuccess(() -> Component.literal("Spawned " + finalSpawned + "x piranport:" + entityId), true);
        return spawned;
    }

    private static int targetFire(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        double range = 32.0;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<FloatingTargetEntity> targets = level.getEntitiesOfClass(
                FloatingTargetEntity.class, searchBox, Entity::isAlive);

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("附近没有浮动靶子"));
            return 0;
        }

        int fired = 0;
        for (FloatingTargetEntity target : targets) {
            MissileEntity missile = new MissileEntity(level,
                    MissileEntity.MissileType.ANTI_SHIP, 20f, 0f, 2.0f,
                    "piranport:sy1_missile");

            double startX = target.getX();
            double startY = target.getY() + target.getBbHeight() / 2;
            double startZ = target.getZ();
            missile.setPos(startX, startY, startZ);
            missile.setOwner(target);

            // Vec3.normalize() 对零长度向量返回 (NaN, NaN, NaN)，而靶子与玩家同点
            // （玩家站进靶子碰撞箱、或刚被指令传送到靶子上）完全可达。
            // 一旦 NaN 进了 setDeltaMovement，实体坐标会在第一 tick 变成 NaN，
            // 之后连带区块/存档一起坏掉——必须在这里挡住。
            Vec3 raw = player.position()
                    .add(0, player.getBbHeight() / 2, 0)
                    .subtract(startX, startY, startZ);
            if (raw.lengthSqr() < 1.0e-6) {
                continue;  // 距离过近，方向无意义，跳过这一个靶子
            }
            Vec3 toPlayer = raw.normalize();
            float speed = MissileEntity.MissileType.ANTI_SHIP.initialSpeed;
            missile.setDeltaMovement(toPlayer.scale(speed));
            missile.setTrackedTarget(player);

            level.addFreshEntity(missile);
            fired++;
        }

        if (fired == 0) {
            source.sendFailure(Component.literal("浮动靶子与你的距离过近，无法确定发射方向（请退开几格）"));
            return 0;
        }

        int finalFired = fired;
        source.sendSuccess(() -> Component.literal(
                "§c⚠ " + finalFired + " 个浮动靶子向你发射了导弹！注意躲避！"), true);
        return fired;
    }

    private static int targetB25(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        // target_b25 会放飞一架真实的 B25 并对玩家区域实施轰炸，属于"会伤害玩家的
        // 破坏性测试指令"，必须与 target_fire 及 model_debug 一样受调试会话门禁约束，
        // 否则任何 OP 都能在普通游戏里凭空召唤轰炸机。而且调试模式必须由"执行者本人"开启。
        if (!PiranPortDebug.isSessionActive(player.getUUID())) {
            source.sendFailure(Component.literal(
                    "§c此指令需要先为你自己开启调试模式（按 F8 开启）"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            source.sendFailure(Component.literal("§c和平难度下轰炸机会被立即清除，请在非和平难度使用"));
            return 0;
        }

        double range = 32.0;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<FloatingTargetEntity> targets = level.getEntitiesOfClass(
                FloatingTargetEntity.class, searchBox, Entity::isAlive);

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("附近没有浮动靶子"));
            return 0;
        }

        // 不设上限的话，"在靶场中央连敲几次指令"会把整个附近的靶子全部放飞 B25，
        // 每次 32 格内的靶子都算一遍，服务端瞬间多出几十架飞机 + 后续几十组投弹。
        int maxLaunch = Math.min(targets.size(), TARGET_B25_MAX_LAUNCH);

        int launched = 0;
        for (int i = 0; i < maxLaunch; i++) {
            FloatingTargetEntity target = targets.get(i);
            ItemStack b25Stack = new ItemStack(ModItems.B25_BOMBER.get());
            Vec3 spawnPos = new Vec3(target.getX(),
                    target.getY() + target.getBbHeight() + 1.0,
                    target.getZ());

            AircraftEntity aircraft = AircraftEntity.createAutonomous(level, spawnPos, b25Stack, null, null);
            level.addFreshEntity(aircraft);
            launched++;
        }

        int finalLaunched = launched;
        source.sendSuccess(() -> Component.literal(
                finalLaunched + " 个浮动靶子放飞了B25轰炸机！"
                + (targets.size() > finalLaunched ? "（已按上限截断，共 " + targets.size() + " 个靶子）" : "")), true);
        return launched;
    }

    /** {@code /ppd target_b25} 单次最多放飞的轰炸机数：够做编队测试，又不至于一把打垮服务端。 */
    private static final int TARGET_B25_MAX_LAUNCH = 8;

    private static int modelDebug(CommandSourceStack source, String modelType, String variant, boolean force) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }
        if (!PiranPortDebug.isSessionActive(player.getUUID())) {
            source.sendFailure(Component.literal(
                    "§c此指令需要先为你自己开启调试模式（按 F8 开启）"));
            return 0;
        }
        // `/ppd model_debug b25 force`（省略 variant）时 force 落在 variant 位上，
        // 把它归一化成"默认档 + 强制"。
        if ("force".equals(variant)) {
            variant = "";
            force = true;
        }
        // 支持表与 ModelDebugBlockEntityRenderer 的 switch 一一对应。
        // 之前这里只放行 b25/f4f，把 heavy_cruiser/light_carrier 也一起挡掉了（补齐时漏改校验）。
        if (!MODEL_DEBUG_TYPES.contains(modelType)) {
            source.sendFailure(Component.literal("Unknown model: " + modelType
                    + " (supported: " + String.join(", ", MODEL_DEBUG_TYPES) + ")"));
            return 0;
        }
        if (!variant.isEmpty() && !MODEL_DEBUG_VARIANTS.contains(variant)) {
            source.sendFailure(Component.literal("Unknown variant: " + variant
                    + " (supported: " + String.join(", ", MODEL_DEBUG_VARIANTS) + ")"));
            return 0;
        }
        // 两份名单各自合法不代表组合合法：shell 只认口径（small/medium/large），
        // missile 只认弹种（anti_air/anti_ship/rocket）。原实现只查两个独立列表，
        // 于是 `/ppd model_debug shell rocket` 会一路通过校验，最后在渲染器的
        // switch 里落进 default —— 玩家得到一块什么都不显示的方块，没有报错。
        if (!variant.isEmpty() && !isValidModelVariant(modelType, variant)) {
            source.sendFailure(Component.literal("§c型号 " + modelType + " 不支持变体 " + variant
                    + "（可用: " + String.join(", ", variantsForModel(modelType)) + "）"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        // Clear 7x11x7 working volume.
        //
        // 破坏性防护：这个循环会无条件抹掉 539 格（含箱子及其内容物、告示牌、机器），
        // 而玩家脚下的坐标是完全自由的 —— 站在队友的建筑里敲一下即不可逆摧毁。
        // 任何"按过 F8"的门禁都拦不住这个（门禁校验的是"你是不是在调试"，
        // 与"这块地是不是你的"正交）。因此只允许覆盖上一次由本指令自己放下的结构：
        // 先探测中心格是不是 MODEL_DEBUG 方块，不是就拒绝并要求显式确认。
        boolean selfPlaced = level.getBlockState(center).is(ModBlocks.MODEL_DEBUG.get());
        if (!force && !selfPlaced && !hasModelDebugNeighbourhood(level, center)) {
            source.sendFailure(Component.literal(
                    "§c拒绝清空 " + center.toShortString() + " 附近的 7×11×7 区域："
                    + "该处没有本指令上次放置的调试结构。"));
            source.sendFailure(Component.literal(
                    "§7若确认要在此施工，请加尾参数 force：/ppd model_debug <model> [variant] force"));
            return 0;
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -3; x <= 3; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -3; z <= 3; z++) {
                    level.setBlock(center.offset(x, y, z), air, 3);
                }
            }
        }

        // Center: model debug block (renders static entity model via BER)
        level.setBlock(center, ModBlocks.MODEL_DEBUG.get().defaultBlockState(), 3);
        BlockEntity centerBe = level.getBlockEntity(center);
        if (centerBe instanceof com.piranport.block.entity.ModelDebugBlockEntity mdbe) {
            mdbe.setModelType(modelType, variant);
        }

        // Support blocks so standing signs can survive
        BlockState support = Blocks.GLOWSTONE.defaultBlockState();
        level.setBlock(center.offset(0, -1, -3), support, 3);
        level.setBlock(center.offset(0, -1,  3), support, 3);
        level.setBlock(center.offset( 3, -1, 0), support, 3);
        level.setBlock(center.offset(-3, -1, 0), support, 3);
        level.setBlock(center.offset(0,  3, 0), support, 3);
        level.setBlock(center.offset(0, -4, 0), support, 3);

        // Six direction signs (rotation points sign face toward center)
        placeDirectionSign(level, center.offset(0, 0, -3),  0, "NORTH", "-Z");
        placeDirectionSign(level, center.offset(0, 0,  3),  8, "SOUTH", "+Z");
        placeDirectionSign(level, center.offset( 3, 0, 0),  4, "EAST",  "+X");
        placeDirectionSign(level, center.offset(-3, 0, 0), 12, "WEST",  "-X");
        placeDirectionSign(level, center.offset(0,  4, 0),  0, "UP",    "+Y");
        placeDirectionSign(level, center.offset(0, -3, 0),  0, "DOWN",  "-Y");

        String finalType = modelType;
        String finalVariant = variant;
        String label = (finalVariant.isEmpty() ? finalType : finalType + "/" + finalVariant).toUpperCase();
        source.sendSuccess(() -> Component.literal(
                "§a已在 " + center.toShortString() + " 放置 " + label
                + " 方向核对结构。看模型机头指向的那块告示牌即可确认 yaw=0 的世界方向。"
                + (finalType.equals("shell") || finalType.equals("missile")
                        ? "（弹体按固定 +Z 摆放，应指向 SOUTH/+Z）" : "")), true);
        return 1;
    }

    /**
     * 工作区是否残留本指令上次放置的结构（用于把"覆盖上一次的调试台"与"在别人的建筑上施工"区分开）。
     *
     * <p>扫 7×11×7 的全部格而不是只查中心：玩家离场再回来、或上次只放了一半就被打断时，
     * 中心格可能已经不是 MODEL_DEBUG 了，但周围还有告示牌/荧石残留 —— 那仍然是本指令的地盘。
     */
    private static boolean hasModelDebugNeighbourhood(ServerLevel level, BlockPos center) {
        BlockState modelDebug = ModBlocks.MODEL_DEBUG.get().defaultBlockState();
        for (int x = -3; x <= 3; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (level.getBlockState(center.offset(x, y, z)).is(ModBlocks.MODEL_DEBUG.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void placeDirectionSign(ServerLevel level, BlockPos pos, int rotation,
                                           String line1, String line2) {
        BlockState signState = Blocks.OAK_SIGN.defaultBlockState()
                .setValue(BlockStateProperties.ROTATION_16, rotation);
        level.setBlock(pos, signState, 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SignBlockEntity sign) {
            SignText text = sign.getFrontText()
                    .setMessage(0, Component.literal(line1))
                    .setMessage(1, Component.literal(line2));
            sign.setText(text, true);
            sign.setChanged();
            level.sendBlockUpdated(pos, signState, signState, 3);
        }
    }

    private static int acceptanceKit(CommandSourceStack source, String type) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        int stacks;
        switch (type) {
            case "blueprint_chest" -> stacks = giveBlueprintChestKit(player);
            case "stove" -> stacks = giveStoveKit(player);
            case "artillery" -> stacks = giveArtilleryKit(player);
            case "deep_ocean" -> stacks = giveDeepOceanKit(player);
            case "ship_girl" -> stacks = giveShipGirlKit(player);
            case "trees" -> stacks = giveTreesKit(player);
            case "all" -> {
                stacks = 0;
                stacks += giveBlueprintChestKit(player);
                stacks += giveStoveKit(player);
                stacks += giveArtilleryKit(player);
                stacks += giveDeepOceanKit(player);
                stacks += giveShipGirlKit(player);
                stacks += giveTreesKit(player);
            }
            default -> {
                source.sendFailure(Component.literal("Unknown acceptance kit: " + type
                        + ". Use: blueprint_chest, stove, artillery, deep_ocean, ship_girl, trees, all"));
                return 0;
            }
        }

        int finalStacks = stacks;
        source.sendSuccess(() -> Component.literal("已发放 " + type
                + " 验收套件（" + finalStacks + " 组物品，背包满则掉落在脚边）"), true);
        return stacks;
    }

    private static int giveBlueprintChestKit(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.BLUEPRINT_CHEST.get(), 1);
        stacks += give(player, ModItems.WEAPON_WORKBENCH.get(), 1);
        stacks += give(player, ModItems.MEDIUM_GUN_BLUEPRINT.get(), 2);
        stacks += give(player, ModItems.LARGE_GUN_BLUEPRINT.get(), 2);
        stacks += give(player, ModItems.CREATIVE_BLUEPRINT.get(), 2);
        stacks += give(player, Items.PAPER, 32);
        return stacks;
    }

    private static int giveStoveKit(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.STOVE.get(), 1);
        stacks += give(player, ModItems.COOKING_POT.get(), 1);
        stacks += give(player, Items.BEEF, 8);
        stacks += give(player, Items.PORKCHOP, 8);
        stacks += give(player, Items.CHICKEN, 8);
        stacks += give(player, Items.COD, 8);
        stacks += give(player, Items.POTATO, 8);
        stacks += give(player, Items.HOPPER, 2);
        return stacks;
    }

    private static int giveArtilleryKit(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.SMALL_SHIP_CORE.get(), 1);
        stacks += give(player, ModItems.LARGE_SHIP_CORE.get(), 1);
        stacks += give(player, ModItems.FUEL.get(), 16);
        stacks += give(player, ModItems.ARTILLERY_CONFIG_TOOL.get(), 1);
        stacks += give(player, ModItems.SINGLE_SMALL_GUN.get(), 1);
        stacks += give(player, ModItems.JAPANESE_127MM_TWIN_GUN.get(), 1);
        stacks += give(player, ModItems.MEDIUM_GUN.get(), 1);
        stacks += give(player, ModItems.LARGE_GUN.get(), 1);
        stacks += give(player, ModItems.GERMAN_TWIN_380MM_GUN.get(), 1);
        stacks += give(player, ModItems.FLOATING_TARGET.get(), 8);
        stacks += giveArtilleryShells(player, ModItems.SMALL_HE_SHELL.get(), ModItems.SMALL_AP_SHELL.get());
        stacks += giveArtilleryShells(player, ModItems.MEDIUM_HE_SHELL.get(), ModItems.MEDIUM_AP_SHELL.get());
        stacks += giveArtilleryShells(player, ModItems.LARGE_HE_SHELL.get(), ModItems.LARGE_AP_SHELL.get());
        return stacks;
    }

    private static int giveArtilleryShells(ServerPlayer player, ItemLike he, ItemLike ap) {
        int stacks = 0;
        stacks += give(player, he, 32);
        stacks += give(player, ap, 32);
        return stacks;
    }

    private static int giveDeepOceanKit(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.ABYSSAL_SEEP.get(), 8);
        stacks += give(player, ModItems.ABYSSAL_REPORT.get(), 16);
        stacks += give(player, ModItems.RAW_ALUMINUM.get(), 32);
        stacks += give(player, ModItems.ALUMINUM_INGOT.get(), 16);
        stacks += give(player, ModItems.AVIATION_FUEL.get(), 16);
        stacks += give(player, ModItems.REPAIR_KIT.get(), 3);
        stacks += give(player, ModItems.EXP_SHELL.get(), 8);
        stacks += giveKeyFragments(player, 2);
        stacks += giveFlags(player);
        stacks += give(player, ModItems.DEEP_OCEAN_SUPPLY_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.DEEP_OCEAN_DESTROYER_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.DEEP_OCEAN_BATTLESHIP_SPAWN_EGG.get(), 1);
        stacks += give(player, ModItems.DEEP_OCEAN_CARRIER_SPAWN_EGG.get(), 1);
        stacks += give(player, ModItems.DEEP_OCEAN_SUBMARINE_SPAWN_EGG.get(), 1);
        return stacks;
    }

    private static int giveShipGirlKit(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.SHIP_GIRL_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.SHIP_GIRL_CONTRACT.get(), 2);
        stacks += give(player, ModItems.RICHELIEU_COMMAND_SWORD.get(), 1);
        stacks += give(player, ModItems.ABYSSAL_REPORT.get(), 16);
        stacks += give(player, ModItems.REPAIR_KIT.get(), 3);
        stacks += give(player, ModItems.EXP_SHELL.get(), 16);
        stacks += give(player, ModItems.AVIATION_FUEL.get(), 16);
        stacks += giveKeyFragments(player, 2);
        stacks += giveFlags(player);
        stacks += giveSkinCores(player);
        return stacks;
    }

    private static int giveTreesKit(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, Items.BONE_MEAL, 64);
        stacks += giveCropSeeds(player);
        stacks += giveTreeSet(player, ModItems.PEACH_LOG.get(), ModItems.PEACH_LEAVES.get(), ModItems.PEACH_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.MAIDENHAIR_LOG.get(), ModItems.MAIDENHAIR_LEAVES.get(), ModItems.MAIDENHAIR_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.SAGO_PALM_LOG.get(), ModItems.SAGO_PALM_LEAVES.get(), ModItems.SAGO_PALM_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.GARDENIA_LOG.get(), ModItems.GARDENIA_LEAVES.get(), ModItems.GARDENIA_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.CHINESE_PLUM_LOG.get(), ModItems.CHINESE_PLUM_LEAVES.get(), ModItems.CHINESE_PLUM_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.MAPPLE_LOG.get(), ModItems.MAPPLE_LEAVES.get(), ModItems.MAPPLE_SAPLING.get());
        return stacks;
    }

    private static int giveCropSeeds(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.TOMATO_SEEDS.get(), 8);
        stacks += give(player, ModItems.SOYBEAN_SEEDS.get(), 8);
        stacks += give(player, ModItems.CHILI_SEEDS.get(), 8);
        stacks += give(player, ModItems.ONION_SEEDS.get(), 8);
        stacks += give(player, ModItems.RICE_SEEDS.get(), 8);
        stacks += give(player, ModItems.LETTUCE_SEEDS.get(), 8);
        stacks += give(player, ModItems.GARLIC_SEEDS.get(), 8);
        stacks += give(player, ModItems.PINEAPPLE_SEED.get(), 8);
        stacks += give(player, ModItems.LABLAB_BEAN_SEEDS.get(), 8);
        stacks += give(player, ModItems.ORMOSIA_SEEDS.get(), 8);
        stacks += give(player, ModItems.CELERY_SEEDS.get(), 8);
        stacks += give(player, ModItems.RYE_SEEDS.get(), 8);
        return stacks;
    }

    private static int giveTreeSet(ServerPlayer player, ItemLike log, ItemLike leaves, ItemLike sapling) {
        int stacks = 0;
        stacks += give(player, log, 8);
        stacks += give(player, leaves, 8);
        stacks += give(player, sapling, 4);
        return stacks;
    }

    /**
     * 发钥匙碎片（ch1~ch7 各 count 个）。
     *
     * <p>原先发的是 9 种无序意志碎片（α~ι）。碎片体系收敛为单一通用货币
     * 「钥匙碎片」后，这里改为把 7 个章节的钥匙碎片各发一份——调试时仍需覆盖
     * 全章节，否则测不了「某章的钥匙合不出来」这类问题。</p>
     */
    private static int giveKeyFragments(ServerPlayer player, int count) {
        int stacks = 0;
        stacks += give(player, ModItems.KEY_FRAGMENT_CH1.get(), count);
        stacks += give(player, ModItems.KEY_FRAGMENT_CH2.get(), count);
        stacks += give(player, ModItems.KEY_FRAGMENT_CH3.get(), count);
        stacks += give(player, ModItems.KEY_FRAGMENT_CH4.get(), count);
        stacks += give(player, ModItems.KEY_FRAGMENT_CH5.get(), count);
        stacks += give(player, ModItems.KEY_FRAGMENT_CH6.get(), count);
        stacks += give(player, ModItems.KEY_FRAGMENT_CH7.get(), count);
        return stacks;
    }

    private static int giveFlags(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.FLAG_J.get(), 1);
        stacks += give(player, ModItems.FLAG_E.get(), 1);
        stacks += give(player, ModItems.FLAG_U.get(), 1);
        stacks += give(player, ModItems.FLAG_G.get(), 1);
        stacks += give(player, ModItems.FLAG_F.get(), 1);
        stacks += give(player, ModItems.FLAG_I.get(), 1);
        stacks += give(player, ModItems.FLAG_C.get(), 1);
        return stacks;
    }

    private static int giveSkinCores(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.SKIN_CORE_4.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_5.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_6.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_7.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_8.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_9.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_10.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_11.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_12.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_13.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_14.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_15.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_16.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_17.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_18.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_19.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_20.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_21.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_22.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_23.get(), 1);
        stacks += give(player, ModItems.SKIN_CORE_24.get(), 1);
        return stacks;
    }

    private static int give(ServerPlayer player, ItemLike item, int count) {
        return give(player, new ItemStack(item, count));
    }

    private static int give(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        ItemStack remaining = stack.copy();
        boolean accepted = player.getInventory().add(remaining);
        if (!accepted && !remaining.isEmpty()) {
            player.drop(remaining, false);
        }
        return 1;
    }

    private static int locateRuin(CommandSourceStack source, String type) {
        // 白名单必须与 spawn_ruin 的建议列表一致。原实现直接把参数拼进 ID 发给玩家，
        // 玩家敲 /ppd locate_ruin xxx 会拿到一条 /locate structure piranport:xxx 的
        // "用法提示"——这个提示指向一个不存在的结构，且永远返回成功，看起来像指令坏了。
        if (!LOCATE_RUIN_TYPES.contains(type)) {
            source.sendFailure(Component.literal("Unknown ruin type: " + type
                    + " (supported: " + String.join(", ", LOCATE_RUIN_TYPES) + ")"));
            return 0;
        }
        String structureId = "piranport:" + type;
        // 结构是否真的注册过要另外查一次：白名单只挡拼写错误，挡不住
        // "名单里有但 worldgen 未注册" 的档位（改名/未启用时就会这样）。
        //
        // 注意必须判 isEmpty()：HolderLookup 的 getOptional 返回 Optional，
        // 未命中时是 Optional.empty() 而**不是 null**，拿 != null 判是死的。
        if (source.getServer().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getOptional(ResourceLocation.fromNamespaceAndPath("piranport", type))
                .isEmpty()) {
            source.sendFailure(Component.literal("§c该结构未注册: " + structureId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Use: /locate structure " + structureId), false);
        return 1;
    }

    /** {@code /ppd locate_ruin} 与 {@code /ppd spawn_ruin} 共用的遗迹名单。 */
    private static final java.util.List<String> LOCATE_RUIN_TYPES = java.util.List.of(
            "portal_ruin", "supply_depot", "outpost", "abyssal_base", "abandoned_portal");
}
