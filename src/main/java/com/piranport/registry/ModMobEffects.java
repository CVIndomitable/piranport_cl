package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.effect.EvasionEffect;
import com.piranport.effect.FloodingEffect;
import com.piranport.effect.ReloadBoostEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 阶段 3：先注册装填加速 / 高速规避两个空 effect 给食物用。
 * 实际行为逻辑等到对应阶段（武器装填 → 阶段 7；闪避 → 阶段 5/7）回填。
 */
public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, PiranPort.MOD_ID);

    public static final RegistryObject<MobEffect> RELOAD_BOOST =
            MOB_EFFECTS.register("reload_boost", ReloadBoostEffect::new);

    public static final RegistryObject<MobEffect> EVASION =
            MOB_EFFECTS.register("evasion", EvasionEffect::new);

    public static final RegistryObject<MobEffect> FLOODING =
            MOB_EFFECTS.register("flooding", FloodingEffect::new);

    private ModMobEffects() {}
}
