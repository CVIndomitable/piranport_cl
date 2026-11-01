package com.piranport.client.gecko;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.util.RenderUtil;

/**
 * 舰装 GeckoLib 模型的动画宿主。
 *
 * <p><b>每个玩家 × 每个模型一份</b>：{@code AttachedAnimatableRenderLayer} 自带的缓存是按
 * Level 共享的单例（内部只有一个 {@code WeakReference}），所有玩家会共用同一套动画状态；
 * 开火动画按玩家触发时就会互相串。这里由 {@link RiggingAttachedLayer} 按玩家维护实例，
 * 本类只负责自己的控制器与实例缓存。
 *
 * <p>控制器只有一个：待机动画常驻，{@code fire} 走 GeckoLib 的 triggerable 动画，
 * 播完自动回到待机。多个控制器同时写同一批骨骼会互相覆盖，所以不拆成两个 controller。
 */
public class RiggingAnimatable implements GeoAnimatable {

    private final RiggingRenderDef def;
    private final AnimatableInstanceCache cache = new InstancedAnimatableInstanceCache(this);
    private final AnimationController<RiggingAnimatable> mainController;

    /** 解析后的待机动画；null 表示还没解析过。 */
    private RawAnimation idleAnimation;

    public RiggingAnimatable(RiggingRenderDef def) {
        this.def = def;
        this.mainController = new AnimationController<>(this, "main", 0, state -> {
            RawAnimation idle = resolveIdleAnimation();
            if (idle != null) {
                state.setAnimation(idle);
            }
            return PlayState.CONTINUE;
        });
        // fire 是 triggerable 动画：控制器平时保持在 STOP，触发时由 GeckoLib 接管，
        // 播完自动清掉并让上面的状态处理器恢复 idle。
        this.mainController.triggerableAnim(
                def.fireAnimation(),
                RawAnimation.begin().thenPlay(def.fireAnimation()));
    }

    public RiggingRenderDef def() {
        return def;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(mainController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        // 不绑定实体的 animatable 走渲染时钟；与 GeckoLib 内部对非 Entity 的取时保持一致。
        return RenderUtil.getCurrentTick();
    }

    /**
     * 触发一次开火动画。Phase 2 还没有开火事件调用它，接口保留给下一阶段。
     *
     * <p>模型没登记 fire 触发项时 GeckoLib 会静默忽略，不会崩。
     */
    public void playFire() {
        AnimatableManager<RiggingAnimatable> manager =
                getAnimatableInstanceCache().getManagerForId(getInstanceId());
        manager.tryTriggerAnimation(def.fireAnimation());
    }

    /**
     * 解析待机动画名：优先 {@code idle}；模型里没有同名动画时回退到第一个动画；
     * 动画文件完全没有动画时返回 null（不播，但也不崩）。
     */
    private RawAnimation resolveIdleAnimation() {
        if (idleAnimation != null) {
            return idleAnimation;
        }
        BakedAnimations baked = GeckoLibCache.getBakedAnimations().get(def.animation());
        if (baked == null || baked.animations().isEmpty()) {
            return null;
        }
        String name = def.idleAnimation();
        if (!baked.animations().containsKey(name)) {
            name = baked.animations().keySet().iterator().next();
        }
        idleAnimation = RawAnimation.begin().thenLoop(name);
        return idleAnimation;
    }

    /** 与 GeckoLib 默认 {@code GeoRenderer#getInstanceId} 相同，管理器本来就是按实例缓存。 */
    private long getInstanceId() {
        return hashCode();
    }
}
