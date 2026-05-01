package com.piranport.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.piranport.registry.ModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Debug commands for PiranPort mod.
 * Registered under /ppd (Piran Port Debug).
 */
public final class PiranPortCommands {

    private PiranPortCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ppd")
                .requires(src -> src.hasPermission(2))

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
        );
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
}
