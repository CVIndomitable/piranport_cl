package com.piranport.combat.cannon;

import com.piranport.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import static com.piranport.combat.cannon.CannonAmmoRules.CaliberFamily;
import static com.piranport.combat.cannon.CannonAmmoRules.familyForWeapon;

/** 火炮开火和装填的音效表现。 */
public final class CannonSounds {
    private CannonSounds() {}

    static float getSoundPitch(ItemStack weapon) {
        return getSoundPitch(weapon, null);
    }

    static float getSoundPitch(ItemStack weapon, @Nullable Level level) {
        return switch (familyForWeapon(weapon, level)) {
            case SMALL -> 1.5f;
            case MEDIUM -> 1.2f;
            case LARGE -> 0.8f;
        };
    }

    static SoundEvent getFireSound(ItemStack weapon) {
        return getFireSound(weapon, null);
    }

    static SoundEvent getFireSound(ItemStack weapon, @Nullable Level level) {
        return switch (familyForWeapon(weapon, level)) {
            case SMALL -> ModSounds.CANNON_FIRE_SMALL.get();
            case MEDIUM -> ModSounds.CANNON_FIRE_MEDIUM.get();
            case LARGE -> ModSounds.CANNON_FIRE_LARGE.get();
        };
    }

    static SoundEvent getFireTailSound(ItemStack weapon) {
        return getFireTailSound(weapon, null);
    }

    static SoundEvent getFireTailSound(ItemStack weapon, @Nullable Level level) {
        return switch (familyForWeapon(weapon, level)) {
            case SMALL -> ModSounds.CANNON_FIRE_SMALL_TAIL.get();
            case MEDIUM -> ModSounds.CANNON_FIRE_MEDIUM_TAIL.get();
            case LARGE -> ModSounds.CANNON_FIRE_LARGE_TAIL.get();
        };
    }

    static SoundEvent getDistantFireSound(ItemStack weapon) {
        return getDistantFireSound(weapon, null);
    }

    static SoundEvent getDistantFireSound(ItemStack weapon, @Nullable Level level) {
        return switch (familyForWeapon(weapon, level)) {
            case MEDIUM -> ModSounds.CANNON_FIRE_MEDIUM_DISTANT.get();
            case LARGE -> ModSounds.CANNON_FIRE_LARGE_DISTANT.get();
            case SMALL -> null;
        };
    }

    static void playCannonFireSound(Level level, Player player, ItemStack weapon) {
        playCannonFireSound(level, (LivingEntity) player, weapon);
    }

    public static void playCannonFireSound(Level level, LivingEntity shooter, ItemStack weapon) {
        float pitch = getSoundPitch(weapon, level);
        SoundEvent fireSound = getFireSound(weapon, level);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                fireSound, SoundSource.PLAYERS, 2.0f, pitch);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                getFireTailSound(weapon, level), SoundSource.PLAYERS, 1.15f, Math.max(0.55f, pitch * 0.82f));
        SoundEvent distantFireSound = getDistantFireSound(weapon, level);
        if (distantFireSound != null) {
            float distantVolume = familyForWeapon(weapon, level) == CaliberFamily.LARGE ? 1.65f : 1.25f;
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                    distantFireSound, SoundSource.PLAYERS, distantVolume, Math.max(0.5f, pitch * 0.62f));
        }
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.95f, pitch * 0.75f);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.65f, pitch * 0.55f);
    }

    static void playCannonReloadStartSound(Player player, ItemStack weapon) {
        float pitch = Math.max(0.55f, getSoundPitch(weapon, player.level()) * 0.75f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CANNON_RELOAD.get(), SoundSource.PLAYERS, 0.45f, pitch);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CANNON_RELOAD_BREECH.get(), SoundSource.PLAYERS, 0.32f, Math.max(0.5f, pitch * 0.88f));
    }

    static void playCannonReloadCompleteSound(Player player, ItemStack weapon) {
        float pitch = Math.max(0.65f, getSoundPitch(weapon, player.level()));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CANNON_RELOAD.get(), SoundSource.PLAYERS, 0.65f, pitch);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CANNON_RELOAD_BREECH.get(), SoundSource.PLAYERS, 0.42f, Math.max(0.55f, pitch * 0.78f));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.35f, pitch + 0.25f);
    }
}
