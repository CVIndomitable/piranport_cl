package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.effect.EvasionEffect;
import com.piranport.effect.FloodingEffect;
import com.piranport.effect.ReloadBoostEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, PiranPort.MOD_ID);

    public static final DeferredHolder<MobEffect, FloodingEffect> FLOODING =
            MOB_EFFECTS.register("flooding", FloodingEffect::new);

    // Phase 26: combat buffs
    public static final DeferredHolder<MobEffect, ReloadBoostEffect> RELOAD_BOOST =
            MOB_EFFECTS.register("reload_boost", ReloadBoostEffect::new);

    public static final DeferredHolder<MobEffect, EvasionEffect> EVASION =
            MOB_EFFECTS.register("evasion", EvasionEffect::new);

    // Football Superstar Set: experience boost
    public static final DeferredHolder<MobEffect, com.piranport.effect.ExperienceBoostEffect> EXPERIENCE_BOOST =
            MOB_EFFECTS.register("experience_boost", com.piranport.effect.ExperienceBoostEffect::new);

    // Phase 27: 策划 §7.7 着火 Buff（"被发现" 2026-09-09 已取消，见战斗/02 决策）
    public static final DeferredHolder<MobEffect, com.piranport.effect.BurningEffect> BURNING =
            MOB_EFFECTS.register("burning", com.piranport.effect.BurningEffect::new);
}
