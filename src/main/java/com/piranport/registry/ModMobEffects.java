package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.effect.FlammableEffect;
import com.piranport.effect.FloodingEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, PiranPort.MOD_ID);

    public static final DeferredHolder<MobEffect, FloodingEffect> FLOODING =
            MOB_EFFECTS.register("flooding", FloodingEffect::new);

    // Phase 22: applied when transformed with fueled aircraft
    public static final DeferredHolder<MobEffect, FlammableEffect> FLAMMABLE =
            MOB_EFFECTS.register("flammable", FlammableEffect::new);
}
