package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.entitycore.ClientEntityCoreData;
import com.piranport.entitycore.EntityCoreDefinition;
import com.piranport.entitycore.EntityCoreDefinitions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class EntityCorePlayerRenderHandler {
    private static final Map<UUID, CachedEntity> CACHE = new HashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (player.isInvisible()) return;

        int coreId = ClientEntityCoreData.getActiveEntityCore(player.getUUID());
        if (coreId <= 0) {
            CACHE.remove(player.getUUID());
            return;
        }

        EntityCoreDefinition definition = EntityCoreDefinitions.get(coreId).orElse(null);
        if (definition == null) {
            CACHE.remove(player.getUUID());
            return;
        }

        Entity standIn = getOrCreateStandIn(player, definition);
        if (standIn == null) return;

        syncStandInState(standIn, player);
        renderStandIn(standIn, player, event);
        event.setCanceled(true);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static Entity getOrCreateStandIn(AbstractClientPlayer player, EntityCoreDefinition definition) {
        CachedEntity cached = CACHE.get(player.getUUID());
        if (cached != null
                && cached.coreId() == definition.id()
                && cached.level() == player.level()
                && !cached.entity().isRemoved()) {
            return cached.entity();
        }

        Entity created = definition.create(player.level());
        if (created == null) {
            PiranPort.LOGGER.warn("Failed to create entity core stand-in for core {}", definition.id());
            CACHE.remove(player.getUUID());
            return null;
        }

        created.setId(-100_000 - player.getId());
        created.setUUID(player.getUUID());
        created.noCulling = true;
        created.setSilent(true);
        created.setNoGravity(true);
        if (created instanceof Mob mob) {
            mob.setNoAi(true);
        }

        CACHE.put(player.getUUID(), new CachedEntity(definition.id(), player.level(), created));
        return created;
    }

    private static void syncStandInState(Entity standIn, AbstractClientPlayer player) {
        standIn.copyPosition(player);
        standIn.tickCount = player.tickCount;
        standIn.xOld = player.xOld;
        standIn.yOld = player.yOld;
        standIn.zOld = player.zOld;
        standIn.xo = player.xo;
        standIn.yo = player.yo;
        standIn.zo = player.zo;
        standIn.yRotO = player.yRotO;
        standIn.xRotO = player.xRotO;
        standIn.walkDist = player.walkDist;
        standIn.walkDistO = player.walkDistO;
        standIn.setDeltaMovement(player.getDeltaMovement());
        standIn.setOnGround(player.onGround());
        standIn.setPose(player.getPose());
        standIn.setShiftKeyDown(player.isShiftKeyDown());
        standIn.setSprinting(player.isSprinting());
        standIn.setSwimming(player.isSwimming());
        standIn.setGlowingTag(player.hasGlowingTag());
        standIn.setRemainingFireTicks(player.getRemainingFireTicks());

        if (standIn instanceof LivingEntity living) {
            syncLivingState(living, player);
        }
    }

    private static void syncLivingState(LivingEntity living, AbstractClientPlayer player) {
        living.yBodyRot = player.yBodyRot;
        living.yBodyRotO = player.yBodyRotO;
        living.yHeadRot = player.yHeadRot;
        living.yHeadRotO = player.yHeadRotO;
        living.attackAnim = player.attackAnim;
        living.oAttackAnim = player.oAttackAnim;
        living.swinging = player.swinging;
        living.swingingArm = player.swingingArm;
        living.swingTime = player.swingTime;
        living.hurtTime = player.hurtTime;
        living.hurtDuration = player.hurtDuration;
        living.deathTime = player.deathTime;
        living.walkAnimation.setSpeed(player.walkAnimation.speed());
        living.walkAnimation.update(player.walkAnimation.speed(), 1.0F);
        living.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem());
        living.setItemInHand(InteractionHand.OFF_HAND, player.getOffhandItem());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderStandIn(Entity standIn, Player player, RenderPlayerEvent.Pre event) {
        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(standIn);
        renderer.render(standIn, player.getYRot(), event.getPartialTick(),
                event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    private record CachedEntity(int coreId, Level level, Entity entity) {}
}
