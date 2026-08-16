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
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> spawnRuin(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))

                // /ppd spawn_abyssal <entity_type> [count]
                .then(Commands.literal("spawn_abyssal")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("deep_ocean_supply");
                                    builder.suggest("deep_ocean_archivist");
                                    builder.suggest("deep_ocean_engineer");
                                    builder.suggest("deep_ocean_navigator");
                                    builder.suggest("deep_ocean_quartermaster");
                                    builder.suggest("deep_ocean_destroyer");
                                    builder.suggest("deep_ocean_light_cruiser");
                                    builder.suggest("deep_ocean_heavy_cruiser");
                                    builder.suggest("deep_ocean_battle_cruiser");
                                    builder.suggest("deep_ocean_battleship");
                                    builder.suggest("deep_ocean_light_carrier");
                                    builder.suggest("deep_ocean_carrier");
                                    builder.suggest("deep_ocean_submarine");
                                    builder.suggest("deep_ocean_flagship");
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

                // /ppd model_debug <model>
                .then(Commands.literal("model_debug")
                        .then(Commands.argument("model", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("b25");
                                    builder.suggest("f4f");
                                    builder.suggest("heavy_cruiser");
                                    builder.suggest("light_carrier");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> modelDebug(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "model")))))

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
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> locateRuin(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))
        );
    }

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
            case "portal_ruin", "supply_depot", "outpost", "abyssal_base" -> 2;
            default -> {
                source.sendFailure(Component.literal("Unknown ruin type: " + type
                        + ". Use: portal_ruin, supply_depot, outpost, abyssal_base"));
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

            AircraftEntity aircraft = AircraftEntity.createAutonomous(level, spawnPos, b25Stack, null);
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
        stacks += give(player, ModItems.SMALL_GUN.get(), 1);
        stacks += give(player, ModItems.MEDIUM_GUN.get(), 1);
        stacks += give(player, ModItems.LARGE_GUN.get(), 1);
        stacks += give(player, ModItems.FRENCH_QUAD_380MM_GUN.get(), 1);
        stacks += give(player, ModItems.SALVO_TEST_GUN.get(), 1);
        stacks += give(player, ModItems.FLOATING_TARGET.get(), 8);
        stacks += giveArtilleryShells(player, ModItems.SMALL_HE_SHELL.get(), ModItems.SMALL_AP_SHELL.get(),
                ModItems.SMALL_GRENADE_SHELL.get(), ModItems.SMALL_FLARE_SHELL.get(), ModItems.SMALL_SMOKE_SHELL.get());
        stacks += giveArtilleryShells(player, ModItems.MEDIUM_HE_SHELL.get(), ModItems.MEDIUM_AP_SHELL.get(),
                ModItems.MEDIUM_GRENADE_SHELL.get(), ModItems.MEDIUM_FLARE_SHELL.get(), ModItems.MEDIUM_SMOKE_SHELL.get());
        stacks += giveArtilleryShells(player, ModItems.LARGE_HE_SHELL.get(), ModItems.LARGE_AP_SHELL.get(),
                ModItems.LARGE_GRENADE_SHELL.get(), ModItems.LARGE_FLARE_SHELL.get(), ModItems.LARGE_SMOKE_SHELL.get());
        return stacks;
    }

    private static int giveArtilleryShells(ServerPlayer player, ItemLike he, ItemLike ap,
                                           ItemLike grenade, ItemLike flare, ItemLike smoke) {
        int stacks = 0;
        stacks += give(player, he, 32);
        stacks += give(player, ap, 32);
        stacks += give(player, grenade, 32);
        stacks += give(player, flare, 16);
        stacks += give(player, smoke, 16);
        return stacks;
    }

    private static int giveDeepOceanKit(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.ABYSSAL_PORTAL_FRAME.get(), 8);
        stacks += give(player, ModItems.PORTAL_ACTIVATION_CORE.get(), 2);
        stacks += give(player, ModItems.ABYSSAL_SEEP.get(), 8);
        stacks += give(player, ModItems.ABYSSAL_REPORT.get(), 16);
        stacks += give(player, ModItems.RAW_ALUMINUM.get(), 32);
        stacks += give(player, ModItems.ALUMINUM_INGOT.get(), 16);
        stacks += give(player, ModItems.AVIATION_FUEL.get(), 16);
        stacks += give(player, ModItems.REPAIR_KIT.get(), 3);
        stacks += give(player, ModItems.EXP_SHELL.get(), 8);
        stacks += giveChaosShards(player, 2);
        stacks += giveFlags(player);
        stacks += give(player, ModItems.DEEP_OCEAN_SUPPLY_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.DEEP_OCEAN_ARCHIVIST_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.DEEP_OCEAN_ENGINEER_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.DEEP_OCEAN_NAVIGATOR_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.DEEP_OCEAN_QUARTERMASTER_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.DEEP_OCEAN_DESTROYER_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.DEEP_OCEAN_BATTLESHIP_SPAWN_EGG.get(), 1);
        stacks += give(player, ModItems.DEEP_OCEAN_CARRIER_SPAWN_EGG.get(), 1);
        stacks += give(player, ModItems.DEEP_OCEAN_SUBMARINE_SPAWN_EGG.get(), 1);
        stacks += give(player, ModItems.DEEP_OCEAN_FLAGSHIP_SPAWN_EGG.get(), 1);
        return stacks;
    }

    private static int giveShipGirlKit(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.SHIP_GIRL_SPAWN_EGG.get(), 2);
        stacks += give(player, ModItems.SHIP_GIRL_CONTRACT.get(), 2);
        stacks += give(player, ModItems.RICHELIEU_COMMAND_SWORD.get(), 1);
        stacks += give(player, ModItems.ABYSSAL_REPORT.get(), 16);
        stacks += give(player, ModItems.REPAIR_KIT.get(), 3);
        stacks += give(player, ModItems.PORTAL_ACTIVATION_CORE.get(), 2);
        stacks += give(player, ModItems.EXP_SHELL.get(), 16);
        stacks += give(player, ModItems.AVIATION_FUEL.get(), 16);
        stacks += giveChaosShards(player, 2);
        stacks += giveFlags(player);
        stacks += giveSkinCores(player);
        return stacks;
    }

    private static int giveTreesKit(ServerPlayer player) {
        int stacks = 0;
        stacks += give(player, ModItems.WILD_GARDEN.get(), 16);
        stacks += give(player, Items.BONE_MEAL, 64);
        stacks += giveCropSeeds(player);
        stacks += giveTreeSet(player, ModItems.PEACH_LOG.get(), ModItems.PEACH_LEAVES.get(), ModItems.PEACH_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.MAIDENHAIR_LOG.get(), ModItems.MAIDENHAIR_LEAVES.get(), ModItems.MAIDENHAIR_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.SAGO_PALM_LOG.get(), ModItems.SAGO_PALM_LEAVES.get(), ModItems.SAGO_PALM_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.GARDENIA_LOG.get(), ModItems.GARDENIA_LEAVES.get(), ModItems.GARDENIA_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.CHINESE_PLUM_LOG.get(), ModItems.CHINESE_PLUM_LEAVES.get(), ModItems.CHINESE_PLUM_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.MAPPLE_LOG.get(), ModItems.MAPPLE_LEAVES.get(), ModItems.MAPPLE_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.CHORUS_TREE_LOG.get(), ModItems.CHORUS_TREE_LEAVES.get(), ModItems.CHORUS_TREE_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.SLIME_TREE_LOG.get(), ModItems.SLIME_TREE_LEAVES.get(), ModItems.SLIME_TREE_SAPLING.get());
        stacks += giveTreeSet(player, ModItems.LAVA_SLIME_TREE_LOG.get(), ModItems.LAVA_SLIME_TREE_LEAVES.get(), ModItems.LAVA_SLIME_TREE_SAPLING.get());
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

    private static int giveChaosShards(ServerPlayer player, int count) {
        int stacks = 0;
        stacks += give(player, ModItems.CHAOS_SHARD_ALPHA.get(), count);
        stacks += give(player, ModItems.CHAOS_SHARD_BETA.get(), count);
        stacks += give(player, ModItems.CHAOS_SHARD_GAMMA.get(), count);
        stacks += give(player, ModItems.CHAOS_SHARD_DELTA.get(), count);
        stacks += give(player, ModItems.CHAOS_SHARD_EPSILON.get(), count);
        stacks += give(player, ModItems.CHAOS_SHARD_ZETA.get(), count);
        stacks += give(player, ModItems.CHAOS_SHARD_ETA.get(), count);
        stacks += give(player, ModItems.CHAOS_SHARD_THETA.get(), count);
        stacks += give(player, ModItems.CHAOS_SHARD_IOTA.get(), count);
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
        // Delegate to vanilla /locate command format
        String structureId = "piranport:" + type;
        source.sendSuccess(() -> Component.literal("Use: /locate structure " + structureId), false);
        return 1;
    }
}
