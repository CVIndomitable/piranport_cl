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
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_FIRE_SMALL_TAIL = register("cannon_fire_small_tail");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_FIRE_MEDIUM_TAIL = register("cannon_fire_medium_tail");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_FIRE_LARGE_TAIL = register("cannon_fire_large_tail");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_FIRE_MEDIUM_DISTANT = register("cannon_fire_medium_distant");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_FIRE_LARGE_DISTANT = register("cannon_fire_large_distant");
    // 装填音效
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_RELOAD = register("cannon_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_RELOAD_BREECH = register("cannon_reload_breech");
    // 爆炸音效
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_EXPLOSION = register("cannon_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_EXPLOSION_DISTANT = register("cannon_explosion_distant");
    // 炮弹呼啸飞行音效
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_WHISTLE = register("shell_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_WHISTLE_FAST = register("shell_whistle_fast");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_WHISTLE_HEAVY = register("shell_whistle_heavy");
    // 弹种切换音效
    public static final DeferredHolder<SoundEvent, SoundEvent> AMMO_SWITCH = register("ammo_switch");
    // 炮口火焰燃烧音效
    public static final DeferredHolder<SoundEvent, SoundEvent> MUZZLE_BURN = register("muzzle_burn");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, name)));
    }
}
