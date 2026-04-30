package com.piranport.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.FloatingTargetEntity;
import com.piranport.entity.MissileEntity;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

                // /ppd target_fire
                .then(Commands.literal("target_fire")
                        .executes(ctx -> targetFire(ctx.getSource())))

                // /ppd target_b25
                .then(Commands.literal("target_b25")
                        .executes(ctx -> targetB25(ctx.getSource())))

                // /ppd model_debug <model>
                .then(Commands.literal("model_debug")
                        .then(Commands.argument("model", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("b25");
                                    builder.suggest("f4f");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> modelDebug(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "model")))))

                // /ppd locate_ruin <type>
                .then(Commands.literal("locate_ruin")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("portal_ruin");
                                    builder.suggest("supply_depot");
                                    builder.suggest("outpost");
                                    builder.suggest("abyssal_base");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> locateRuin(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))
        );
    }

    private static int spawnRuin(CommandSourceStack source, String type) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        // Build a simple placeholder structure at the player's position
        int size;
        switch (type) {
            case "portal_ruin" -> size = 8;
            case "supply_depot" -> size = 12;
            case "outpost" -> size = 24;
            case "abyssal_base" -> size = 40;
            default -> {
                source.sendFailure(Component.literal("Unknown ruin type: " + type
                        + ". Use: portal_ruin, supply_depot, outpost, abyssal_base"));
                return 0;
            }
        }

        // Build a simple stone brick platform with walls
        BlockState floor = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState wall = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState frame = ModBlocks.ABYSSAL_PORTAL_FRAME.get().defaultBlockState();

        for (int x = -size / 2; x <= size / 2; x++) {
            for (int z = -size / 2; z <= size / 2; z++) {
                BlockPos floorPos = center.offset(x, -1, z);
                level.setBlock(floorPos, floor, 3);

                // Walls on perimeter
                boolean isEdge = x == -size / 2 || x == size / 2 || z == -size / 2 || z == size / 2;
                if (isEdge) {
                    for (int y = 0; y < 3; y++) {
                        level.setBlock(floorPos.above(y + 1), wall, 3);
                    }
                }
            }
        }

        // Place portal frame corners for portal_ruin type
        if ("portal_ruin".equals(type)) {
            for (int y = 0; y < 4; y++) {
                level.setBlock(center.offset(-2, y, 0), frame, 3);
                level.setBlock(center.offset(2, y, 0), frame, 3);
            }
            level.setBlock(center.offset(-1, 3, 0), frame, 3);
            level.setBlock(center.offset(0, 3, 0), frame, 3);
            level.setBlock(center.offset(1, 3, 0), frame, 3);
        }

        // Place a chest with the appropriate loot table
        BlockPos chestPos = center.offset(size / 4, 0, size / 4);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);

        source.sendSuccess(() -> Component.literal("Spawned placeholder " + type
                + " at " + center.toShortString()), true);
        return 1;
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
        java.util.UUID cluster = java.util.UUID.randomUUID();
        com.piranport.npc.ai.FleetGroupManager mgr = com.piranport.npc.ai.FleetGroupManager.get(level);
        mgr.createGroup(cluster);

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Entity entity = optType.get().create(level);
            if (entity == null) continue;

            double offsetX = (level.random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 6.0;
            entity.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);

            if (entity instanceof Mob mob) {
                EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(player.blockPosition()),
                        MobSpawnType.COMMAND, null);
            }

            if (entity instanceof AbstractDeepOceanEntity abyssal) {
                abyssal.setFleetGroupId(cluster);
                mgr.addMember(cluster, abyssal.getUUID());
            }

            level.addFreshEntity(entity);
            spawned++;
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

            Vec3 toPlayer = player.position()
                    .add(0, player.getBbHeight() / 2, 0)
                    .subtract(startX, startY, startZ)
                    .normalize();
            float speed = MissileEntity.MissileType.ANTI_SHIP.initialSpeed;
            missile.setDeltaMovement(toPlayer.scale(speed));
            missile.setTrackedTarget(player);

            level.addFreshEntity(missile);
            fired++;
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

        ServerLevel level = player.serverLevel();
        double range = 32.0;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<FloatingTargetEntity> targets = level.getEntitiesOfClass(
                FloatingTargetEntity.class, searchBox, Entity::isAlive);

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("附近没有浮动靶子"));
            return 0;
        }

        int launched = 0;
        for (FloatingTargetEntity target : targets) {
            ItemStack b25Stack = new ItemStack(ModItems.B25_BOMBER.get());
            Vec3 spawnPos = new Vec3(target.getX(),
                    target.getY() + target.getBbHeight() + 1.0,
                    target.getZ());

            AircraftEntity aircraft = AircraftEntity.createAutonomous(level, spawnPos, b25Stack);
            level.addFreshEntity(aircraft);
            launched++;
        }

        int finalLaunched = launched;
        source.sendSuccess(() -> Component.literal(
                finalLaunched + " 个浮动靶子放飞了B25轰炸机！"), true);
        return launched;
    }

    private static int modelDebug(CommandSourceStack source, String modelType) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }
        if (!PiranPortDebug.isServerEnabled()) {
            source.sendFailure(Component.literal(
                    "§c此指令需要先开启调试模式（按 F8 开启）"));
            return 0;
        }
        if (!"b25".equals(modelType) && !"f4f".equals(modelType)) {
            source.sendFailure(Component.literal("Unknown model: " + modelType + " (supported: b25, f4f)"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        // Clear 7x11x7 working volume
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
            mdbe.setModelType(modelType);
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
        source.sendSuccess(() -> Component.literal(
                "§a已在 " + center.toShortString() + " 放置 " + finalType.toUpperCase()
                + " 方向核对结构。看模型机头指向的那块告示牌即可确认 yaw=0 的世界方向。"), true);
        return 1;
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

    private static int locateRuin(CommandSourceStack source, String type) {
        // Delegate to vanilla /locate command format
        String structureId = "piranport:" + type;
        source.sendSuccess(() -> Component.literal("Use: /locate structure " + structureId), false);
        return 1;
    }
}
