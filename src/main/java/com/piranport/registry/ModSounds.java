package com.piranport.registry;

import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, PiranPort.MOD_ID);

    // 发射音效（按口径分级）
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_FIRE_SMALL = register("cannon_fire_small");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_FIRE_MEDIUM = register("cannon_fire_medium");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_FIRE_LARGE = register("cannon_fire_large");
    // 装填音效
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_RELOAD = register("cannon_reload");
    // 爆炸音效
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_EXPLOSION = register("cannon_explosion");
    // 炮弹呼啸飞行音效
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_WHISTLE = register("shell_whistle");
    // 弹种切换音效
    public static final DeferredHolder<SoundEvent, SoundEvent> AMMO_SWITCH = register("ammo_switch");
    // 炮口火焰燃烧音效
    public static final DeferredHolder<SoundEvent, SoundEvent> MUZZLE_BURN = register("muzzle_burn");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, name)));
    }
}
