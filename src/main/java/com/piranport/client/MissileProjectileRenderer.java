package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.entity.MissileEntity;
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
 * 导弹渲染器：把飞行中的防空/反舰导弹按速度方向对齐，用 3D 弹体模型绘制。
 *
 * <p>定位逻辑与 {@link CannonProjectileRenderer} 同源：先绕 YP 转 yaw、再绕 XP 转 -pitch，
 * 模型的局部 +Z 轴正好被映射到速度方向，弹尖自动超前。
 *
 * <p>档位判断只能用 {@code entity.getItem()}，不能用 {@code MissileEntity.MissileType}：
 * 后者是服务端私有字段、没有 getter、也没进 SynchedEntityData，客户端实体由无参构造函数构造，
 * 永远读到默认的 ANTI_SHIP。而 displayItemId 在实体构造时写入、并随 {@link
 * net.minecraft.world.entity.projectile.ThrowableItemProjectile#getItem()} 同步到客户端，
 * 因此用物品身份做判据才是可靠的。
 *
 * <p>真实导弹物品（anti_air_missile / harpoon_missile 等）的图标保持原有 2D 贴图不变，
 * 3D 模型由隐藏载体物品 {@code missile_projectile_*} 承载，渲染时临时构造其 ItemStack。
 * 火箭弹（rocket_ammo）不在其中——它继续走 {@code ThrownItemRenderer} 的 2D 精灵外观。
 */
public class MissileProjectileRenderer extends EntityRenderer<MissileEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/item/shell_projectile.png");

    // 目标世界尺寸（策划口径）：与同细度的炮弹一致——防空照搬小口径、反舰照搬中口径，
    // 长度天然是炮弹的两倍（模型 26 格 = 1.625 方块）。
    private static final float ANTI_AIR_SCALE = 0.75f;
    private static final float ANTI_SHIP_SCALE = 0.85f;

    private final ItemRenderer itemRenderer;

    public MissileProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.05f;
    }

    /**
     * 判断该弹药物品是否用 3D 反舰弹体渲染。
     * 未列出的一律按反舰处理——实体默认物品回退到 SY1_MISSILE（反舰），保持与 resolveDisplayItem 一致。
     */
    private static boolean isAntiShip(ItemStack ammo) {
        return !ammo.is(ModItems.TERRIER_MISSILE) && !ammo.is(ModItems.ANTI_AIR_MISSILE);
    }

    /** 火箭弹保留原版 2D 精灵外观，不走 3D 弹体。 */
    private static boolean isRocket(ItemStack ammo) {
        return ammo.is(ModItems.ROCKET_AMMO);
    }

    @Override
    public void render(MissileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ItemStack ammo = entity.getItem();
        if (isRocket(ammo)) {
            // 火箭弹：交回原版精灵渲染，位置与朝向由原版逻辑处理
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        boolean antiShip = isAntiShip(ammo);
        ItemStack renderStack = new ItemStack(antiShip
                ? ModItems.MISSILE_PROJECTILE_ANTI_SHIP.get()
                : ModItems.MISSILE_PROJECTILE_ANTI_AIR.get());
        float scale = antiShip ? ANTI_SHIP_SCALE : ANTI_AIR_SCALE;

        poseStack.pushPose();
        Vec3 velocity = entity.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (velocity.lengthSqr() > 1.0e-6) {
            float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG);
            float pitch = (float) (Mth.atan2(velocity.y, horizontal) * Mth.RAD_TO_DEG);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        }
        poseStack.scale(scale, scale, scale);
        // 用 NONE 而非 GROUND：显示变换会右乘在速度定向之后，GROUND 自带的旋转/缩放会污染朝向。
        itemRenderer.renderStatic(renderStack, ItemDisplayContext.NONE, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        return TEXTURE;
    }
}
