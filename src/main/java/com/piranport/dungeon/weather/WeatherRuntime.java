package com.piranport.dungeon.weather;

import com.piranport.PiranPort;
import com.piranport.config.ModCommonConfig;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.SceneData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.npc.deepocean.DeepOceanSubmarineEntity;
import com.piranport.npc.deepocean.DeepOceanTaihouEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** 副本天气的服务端运行时，规则见《副本/27》；纯判定在 {@link WeatherRules}。 */
public final class WeatherRuntime {

    private static final int EFFECT_PERIOD_TICKS = 20;
    private static final int GLOW_DURATION_TICKS = 40;
    private static final String INSTANCE_TAG_PREFIX = "dungeon_instance_";
    private static final String NODE_TAG_PREFIX = "dungeon_node_";
    private static final ResourceLocation FOLLOW_RANGE_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "weather_follow_range");

    private WeatherRuntime() {}

    /** 节点的实际生效天气：大凤在场时为白天。 */
    public static SceneData effectiveScene(ServerLevel level, DungeonInstance instance, String nodeId) {
        SceneData raw = rawScene(instance, nodeId);
        boolean taihouOnField = raw != null && raw != SceneData.DAY
                && !level.getEntitiesOfClass(DeepOceanTaihouEntity.class, usableBox(level, instance),
                        e -> e.isAlive() && hasNodeTags(e, instance, nodeId)).isEmpty();
        return WeatherRules.effectiveScene(raw, taihouOnField);
    }

    public static boolean blocksLaunch(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return false;
        return WeatherRules.blocksLaunch(currentScene(serverLevel, serverPlayer));
    }

    public static boolean blocksLanding(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) return false;
        return WeatherRules.blocksLanding(currentScene(serverLevel, owner));
    }

    /** 大凤「晴空万里」对同队伍航空炸弹、航空鱼雷的减伤判定。 */
    public static boolean isProtectedByTaihou(ServerLevel level, AbstractDeepOceanEntity victim) {
        UUID group = victim.getFleetGroupId();
        DungeonInstance instance = instanceOf(level, victim);
        if (group == null || instance == null) return false;
        return !level.getEntitiesOfClass(DeepOceanTaihouEntity.class, usableBox(level, instance),
                t -> t.isAlive() && group.equals(t.getFleetGroupId())).isEmpty();
    }

    public static void tick(ServerLevel level, DungeonInstance instance) {
        if (instance.getState() != DungeonInstance.State.ACTIVE || instance.getCurrentNode() == null) return;
        String nodeId = instance.getCurrentNode();

        if (WeatherRules.isLightningTick(level.getGameTime(),
                ModCommonConfig.WEATHER_STORM_LIGHTNING_PERIOD_TICKS.get())) {
            strikeLightning(level, instance, nodeId);
        }
        if (level.getGameTime() % EFFECT_PERIOD_TICKS == 0) {
            applyEnemyEffects(level, instance, nodeId, effectiveScene(level, instance, nodeId));
        }
    }

    private static void strikeLightning(ServerLevel level, DungeonInstance instance, String nodeId) {
        if (effectiveScene(level, instance, nodeId) != SceneData.STORM) return;
        float damage = ModCommonConfig.WEATHER_STORM_LIGHTNING_DAMAGE.get().floatValue();
        int oneIn = ModCommonConfig.WEATHER_STORM_LIGHTNING_ONE_IN.get();
        for (ServerPlayer player : DungeonInstanceManager.get(level).getPresentPlayers(instance, level.getServer())) {
            if (level.getRandom().nextInt(oneIn) != 0) continue;
            player.hurt(level.damageSources().lightningBolt(), damage);
        }
    }

    private static void applyEnemyEffects(ServerLevel level, DungeonInstance instance, String nodeId, SceneData scene) {
        double followMultiplier = WeatherRules.followRangeMultiplier(scene,
                ModCommonConfig.WEATHER_FOG_FOLLOW_RANGE_MULT.get(),
                ModCommonConfig.WEATHER_HOT_SUN_FOLLOW_RANGE_MULT.get());
        boolean glowAll = WeatherRules.glowsEnemies(scene);
        boolean glowSubmarines = WeatherRules.highlightsSubmarines(scene);

        for (AbstractDeepOceanEntity enemy : level.getEntitiesOfClass(AbstractDeepOceanEntity.class,
                usableBox(level, instance), e -> e.isAlive() && hasInstanceTag(e, instance))) {
            boolean inNode = hasNodeTags(enemy, instance, nodeId);
            setFollowRangeMultiplier(enemy, inNode ? followMultiplier : 1.0);
            boolean glow = inNode && (glowAll || (glowSubmarines && enemy instanceof DeepOceanSubmarineEntity));
            if (glow) {
                enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, false, false));
            }
        }
    }

    private static void setFollowRangeMultiplier(AbstractDeepOceanEntity enemy, double multiplier) {
        AttributeInstance attribute = enemy.getAttribute(Attributes.FOLLOW_RANGE);
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(FOLLOW_RANGE_MODIFIER);
        if (Math.abs(multiplier - 1.0) < 1.0e-9) {
            if (current != null) attribute.removeModifier(FOLLOW_RANGE_MODIFIER);
            return;
        }
        double amount = multiplier - 1.0;
        if (current != null && Math.abs(current.amount() - amount) < 1.0e-9) return;
        attribute.removeModifier(FOLLOW_RANGE_MODIFIER);
        attribute.addTransientModifier(new AttributeModifier(FOLLOW_RANGE_MODIFIER, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static SceneData currentScene(ServerLevel level, ServerPlayer player) {
        DungeonInstance instance = DungeonInstanceManager.get(level).getInstanceForPlayer(player);
        if (instance == null || instance.getCurrentNode() == null) return SceneData.DAY;
        return effectiveScene(level, instance, instance.getCurrentNode());
    }

    private static SceneData rawScene(DungeonInstance instance, String nodeId) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        NodeData node = stage == null ? null : stage.nodes().get(nodeId);
        return node == null ? null : node.scene();
    }

    private static DungeonInstance instanceOf(ServerLevel level, Entity entity) {
        for (String tag : entity.getTags()) {
            if (tag.startsWith(INSTANCE_TAG_PREFIX)) {
                return DungeonInstanceManager.get(level).getInstance(UUID.fromString(tag.substring(INSTANCE_TAG_PREFIX.length())));
            }
        }
        return null;
    }

    private static boolean hasInstanceTag(Entity entity, DungeonInstance instance) {
        return entity.getTags().contains(INSTANCE_TAG_PREFIX + instance.getInstanceId());
    }

    private static boolean hasNodeTags(Entity entity, DungeonInstance instance, String nodeId) {
        return hasInstanceTag(entity, instance) && entity.getTags().contains(NODE_TAG_PREFIX + nodeId);
    }

    private static AABB usableBox(ServerLevel level, DungeonInstance instance) {
        return new AABB(instance.getUsableMinX(), level.getMinBuildHeight(), instance.getUsableMinZ(),
                instance.getUsableMaxX() + 1, level.getMaxBuildHeight(), instance.getUsableMaxZ() + 1);
    }
}
