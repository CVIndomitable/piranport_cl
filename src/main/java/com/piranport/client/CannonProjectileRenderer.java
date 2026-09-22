package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 火炮炮弹渲染器：把炮弹按飞行速度方向对齐，用 3D 锥形模型绘制。
 *
 * <p>旧版直接渲染掉落物精灵（2D 平面贴图）并按速度旋转，极速飞行时会抖成一条线。
 * 现在按装填的炮弹口径换成 {piranport:custom/shell_*} 三档 3D 锥形模型（弹尖朝 +Z），
 * 与这里"先绕 YP 转 yaw、再绕 XP 转 -pitch"的定向逻辑天然吻合：
 * 模型的局部 +Z 轴正好被映射到速度方向，弹尖自动超前。
 *
 * <p>真实炮弹物品（small_he_shell 等）的图标保持原有 2D 贴图不变，
 * 3D 模型由隐藏载体物品 {@code shell_projectile_*} 承载，渲染时临时构造其 ItemStack。
 */
public class CannonProjectileRenderer extends EntityRenderer<CannonProjectileEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/item/shell_projectile.png");

    // 口径档位一律查数据包标签（与 CannonAmmoRules.matchesCaliber 同源），
    // 不写死物品 id —— 这样数据包新增兼容弹药时渲染档位自动跟随，不会与实际装填规则脱节。

    // 目标世界尺寸（策划口径）：大口径约 0.25 x 1 方块，中小口径依次更细更短。
    // 模型固有尺寸：长 13 格 = 0.8125 方块，粗细 3/4/5 格。
    private static final float SMALL_SCALE = 0.75f;
    private static final float MEDIUM_SCALE = 0.85f;
    private static final float LARGE_SCALE = 0.95f;

    private final ItemRenderer itemRenderer;

    public CannonProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.05f;
    }

    /** 依据已装填的炮弹物品判断口径档位：0 = 小，1 = 中，2 = 大。未列出的按大口径。 */
    private static int tierOf(ItemStack shell) {
        if (shell.is(ShipCoreItem.SMALL_SHELLS)) return 0;
        if (shell.is(ShipCoreItem.MEDIUM_SHELLS)) return 1;
        return 2;
    }

    private static ItemStack renderStackFor(int tier) {
        return switch (tier) {
            case 0 -> new ItemStack(ModItems.SHELL_PROJECTILE_SMALL.get());
            case 1 -> new ItemStack(ModItems.SHELL_PROJECTILE_MEDIUM.get());
            default -> new ItemStack(ModItems.SHELL_PROJECTILE_LARGE.get());
        };
    }

    private static float scaleFor(int tier) {
        return switch (tier) {
            case 0 -> SMALL_SCALE;
            case 1 -> MEDIUM_SCALE;
            default -> LARGE_SCALE;
        };
    }

    @Override
    public void render(CannonProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        Vec3 velocity = entity.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (velocity.lengthSqr() > 1.0e-6) {
            float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG);
            float pitch = (float) (Mth.atan2(velocity.y, horizontal) * Mth.RAD_TO_DEG);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        }

        ItemStack shell = entity.getItem();
        int tier = tierOf(shell);
        ItemStack renderStack = renderStackFor(tier);
        float scale = scaleFor(tier);
        poseStack.scale(scale, scale, scale);
        // 用 NONE 而非 GROUND：显示变换会右乘在速度定向之后，GROUND 自带的旋转/缩放会污染朝向。
        // NONE 是单位变换，模型姿态完全由上面的速度定向决定。
        itemRenderer.renderStatic(renderStack, ItemDisplayContext.NONE, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CannonProjectileEntity entity) {
        return TEXTURE;
    }
}
