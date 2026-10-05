package com.piranport.dungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.dungeon.block.LecternPattern;
import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 副本书台上方悬浮字幕（副本/17 §3.2）：关卡名、通关情况、当前人数/进本门槛。
 * 空书台不显示；建造中/满员额外加一行状态。数据全部来自服务端同步的 BE 字段。
 */
public class DungeonLecternRenderer implements BlockEntityRenderer<DungeonLecternBlockEntity> {

    /** 只在这个距离内绘制，和原版名牌距离同量级。 */
    private static final double MAX_DIST_SQ = 16 * 16;
    private static final float SCALE = 0.025f;
    private static final int LINE_HEIGHT = 10;

    private final Font font;

    public DungeonLecternRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(DungeonLecternBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        LecternPattern.Status status = be.getStatus();
        if (status == LecternPattern.Status.EMPTY) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null
                || mc.player.distanceToSqr(be.getBlockPos().getCenter()) > MAX_DIST_SQ) return;

        List<Component> lines = new ArrayList<>();
        String stage = be.getLabelStage();
        lines.add(stage.isEmpty()
                ? Component.translatable("block.piranport.dungeon_lectern.label.unknown_stage")
                : Component.literal(stage));
        if (status != LecternPattern.Status.INVALID) {
            lines.add(be.isLabelCompleted()
                    ? Component.translatable("block.piranport.dungeon_lectern.label.cleared")
                    : Component.translatable("block.piranport.dungeon_lectern.label.not_cleared",
                            be.getLabelCleared()));
            lines.add(Component.translatable("block.piranport.dungeon_lectern.label.players",
                    be.getLabelPresent(), LecternPattern.instanceCapacity()));
        }
        switch (status) {
            case BUILDING -> lines.add(Component.translatable("block.piranport.dungeon_lectern.label.building"));
            case FULL -> lines.add(Component.translatable("block.piranport.dungeon_lectern.label.full"));
            case INVALID -> lines.add(Component.translatable("block.piranport.dungeon_lectern.label.invalid"));
            default -> { }
        }

        pose.pushPose();
        pose.translate(0.5, 1.6 + lines.size() * LINE_HEIGHT * SCALE, 0.5);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(SCALE, -SCALE, SCALE);
        Matrix4f matrix = pose.last().pose();
        int bg = (int) (mc.options.getBackgroundOpacity(0.25f) * 255.0f) << 24;
        int light = LightTexture.FULL_BRIGHT;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            float x = -font.width(line) / 2f;
            float y = i * LINE_HEIGHT;
            int color = (i == 0) ? 0xFFFFFFFF : statusColor(status, i == lines.size() - 1);
            font.drawInBatch(line, x, y, color, false, matrix, buffers,
                    Font.DisplayMode.SEE_THROUGH, bg, light);
        }
        pose.popPose();
    }

    /** 状态行用纹路同色，其余行灰白。 */
    private static int statusColor(LecternPattern.Status status, boolean lastLine) {
        if (lastLine && status != LecternPattern.Status.READY) {
            return 0xFF000000 | LecternPattern.of(status).tint();
        }
        return 0xFFD0D0D0;
    }

    @Override
    public boolean shouldRenderOffScreen(DungeonLecternBlockEntity be) {
        return true;
    }
}
