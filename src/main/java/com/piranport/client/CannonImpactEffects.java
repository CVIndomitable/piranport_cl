package com.piranport.client;

import com.piranport.network.CannonImpactEffectPayload;
import com.piranport.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 客户端远距炮弹命中特效。 */
public final class CannonImpactEffects {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<ActiveEffect> ACTIVE_EFFECTS = new CopyOnWriteArrayList<>();
    private static final List<PendingSound> PENDING_SOUNDS = new CopyOnWriteArrayList<>();
    private static final double SOUND_DELAY_FREE_DISTANCE = 32.0;
    private static final double SOUND_BLOCKS_PER_TICK = 17.0;
    private static final int MAX_SOUND_DELAY_TICKS = 80;

    private CannonImpactEffects() {}

    public static void spawn(CannonImpactEffectPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        float scale = Math.max(0.6f, Math.min(payload.power(), 4.0f));
        switch (payload.kind()) {
            case HE -> spawnHe(level, payload.x(), payload.y(), payload.z(), scale);
            case AP -> spawnAp(level, payload.x(), payload.y(), payload.z(), scale);
            case VT -> spawnVt(level, payload.x(), payload.y(), payload.z(), scale);
            case WATER -> spawnWater(level, payload.x(), payload.y(), payload.z(), scale);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            ACTIVE_EFFECTS.clear();
            PENDING_SOUNDS.clear();
            return;
        }

        Iterator<PendingSound> soundIterator = PENDING_SOUNDS.iterator();
        while (soundIterator.hasNext()) {
            PendingSound sound = soundIterator.next();
            sound.tick(level);
            if (sound.isDone()) {
                PENDING_SOUNDS.remove(sound);
            }
        }

        Iterator<ActiveEffect> iterator = ACTIVE_EFFECTS.iterator();
        while (iterator.hasNext()) {
            ActiveEffect effect = iterator.next();
            effect.tick(level);
            if (effect.isDone()) {
                ACTIVE_EFFECTS.remove(effect);
            }
        }
    }

    public static void clear() {
        ACTIVE_EFFECTS.clear();
        PENDING_SOUNDS.clear();
    }

    private static void spawnHe(ClientLevel level, double x, double y, double z, float scale) {
        add(level, ParticleTypes.FLASH, x, y + 0.25, z, 0, 0, 0);
        int flameCount = Math.round(4 + scale * 2);
        for (int i = 0; i < flameCount; i++) {
            add(level, ParticleTypes.FLAME,
                    x + spread(scale * 0.35), y + 0.1 + RANDOM.nextDouble() * scale * 0.45, z + spread(scale * 0.35),
                    spread(0.03), 0.02 + RANDOM.nextDouble() * 0.05, spread(0.03));
        }
        spawnSmokeColumn(level, x, y, z, scale, Math.round(10 + scale * 5), false);
        ACTIVE_EFFECTS.add(new ActiveEffect(x, y, z, scale, 44, false));
        scheduleExplosionSound(level, x, y, z,
                Math.min(2.4f, 1.0f + scale * 0.35f), 0.68f + RANDOM.nextFloat() * 0.08f);
    }

    private static void spawnAp(ClientLevel level, double x, double y, double z, float scale) {
        add(level, ParticleTypes.FLASH, x, y + 0.1, z, 0, 0, 0);
        int sparkCount = Math.round(2 + scale);
        for (int i = 0; i < sparkCount; i++) {
            add(level, ParticleTypes.SMALL_FLAME,
                    x + spread(0.18), y + RANDOM.nextDouble() * 0.25, z + spread(0.18),
                    spread(0.04), 0.01 + RANDOM.nextDouble() * 0.03, spread(0.04));
        }
        spawnSmokeColumn(level, x, y, z, Math.max(0.5f, scale * 0.55f), Math.round(4 + scale * 2), false);
        ACTIVE_EFFECTS.add(new ActiveEffect(x, y, z, Math.max(0.5f, scale * 0.45f), 22, false));
        scheduleExplosionSound(level, x, y, z,
                Math.min(1.2f, 0.45f + scale * 0.16f), 0.78f + RANDOM.nextFloat() * 0.08f);
    }

    private static void spawnVt(ClientLevel level, double x, double y, double z, float scale) {
        spawnSmokeColumn(level, x, y, z, scale * 0.85f, Math.round(12 + scale * 4), true);
        ACTIVE_EFFECTS.add(new ActiveEffect(x, y, z, scale * 0.75f, 36, true));
        scheduleExplosionSound(level, x, y, z,
                Math.min(1.8f, 0.75f + scale * 0.22f), 0.58f + RANDOM.nextFloat() * 0.07f);
    }

    private static void spawnWater(ClientLevel level, double x, double y, double z, float scale) {
        int splashCount = Math.round(18 + scale * 10);
        for (int i = 0; i < splashCount; i++) {
            double radius = 0.2 + RANDOM.nextDouble() * scale * 0.5;
            add(level, ParticleTypes.SPLASH,
                    x + spread(radius), y + 0.1, z + spread(radius),
                    spread(0.08), 0.18 + RANDOM.nextDouble() * 0.18, spread(0.08));
        }
        int bubbleCount = Math.round(10 + scale * 5);
        for (int i = 0; i < bubbleCount; i++) {
            add(level, ParticleTypes.BUBBLE,
                    x + spread(scale * 0.45), y - RANDOM.nextDouble() * 0.6, z + spread(scale * 0.45),
                    spread(0.02), 0.03 + RANDOM.nextDouble() * 0.04, spread(0.02));
        }
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.45 + ring * 0.35 * scale;
            int points = 12 + ring * 6;
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0 * i) / points;
                add(level, ParticleTypes.FISHING,
                        x + Math.cos(angle) * radius, y + 0.03, z + Math.sin(angle) * radius,
                        Math.cos(angle) * 0.025, 0.01, Math.sin(angle) * 0.025);
            }
        }
        level.playLocalSound(x, y, z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS,
                Math.min(2.0f, 0.9f + scale * 0.25f), 0.75f + RANDOM.nextFloat() * 0.12f, false);
    }

    private static void spawnSmokeColumn(ClientLevel level, double x, double y, double z,
            float scale, int count, boolean blackSmoke) {
        for (int i = 0; i < count; i++) {
            double height = RANDOM.nextDouble() * (1.4 + scale * 1.2);
            double radius = (0.25 + scale * 0.22) * (0.5 + RANDOM.nextDouble());
            ParticleOptions type = blackSmoke || RANDOM.nextBoolean()
                    ? ParticleTypes.LARGE_SMOKE
                    : ParticleTypes.CAMPFIRE_COSY_SMOKE;
            add(level, type,
                    x + spread(radius), y + 0.25 + height, z + spread(radius),
                    spread(0.025), 0.025 + RANDOM.nextDouble() * 0.045, spread(0.025));
        }
    }

    private static void add(ClientLevel level, ParticleOptions type, double x, double y, double z,
            double dx, double dy, double dz) {
        level.addAlwaysVisibleParticle(type, true, x, y, z, dx, dy, dz);
    }

    private static void scheduleExplosionSound(ClientLevel level, double x, double y, double z,
                                               float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        int delay = 0;
        if (mc.player != null) {
            double distance = mc.player.distanceToSqr(x, y, z);
            distance = Math.sqrt(distance);
            if (distance > SOUND_DELAY_FREE_DISTANCE) {
                delay = (int) Math.min(MAX_SOUND_DELAY_TICKS,
                        Math.round((distance - SOUND_DELAY_FREE_DISTANCE) / SOUND_BLOCKS_PER_TICK));
            }
        }
        if (delay <= 0) {
            play(level, x, y, z, volume, pitch);
        } else {
            PENDING_SOUNDS.add(new PendingSound(x, y, z, volume, pitch, delay));
        }
    }

    private static void play(ClientLevel level, double x, double y, double z, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        boolean distant = mc.player != null
                && mc.player.distanceToSqr(x, y, z) > SOUND_DELAY_FREE_DISTANCE * SOUND_DELAY_FREE_DISTANCE;
        SoundEvent sound = distant ? ModSounds.CANNON_EXPLOSION_DISTANT.get() : ModSounds.CANNON_EXPLOSION.get();
        float adjustedVolume = distant ? volume * 0.82f : volume;
        float adjustedPitch = distant ? Math.max(0.55f, pitch * 0.82f) : pitch;
        level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, adjustedVolume, adjustedPitch, false);
    }

    private static double spread(double radius) {
        return (RANDOM.nextDouble() - 0.5) * 2.0 * radius;
    }

    private static final class ActiveEffect {
        private final double x;
        private final double y;
        private final double z;
        private final float scale;
        private final int lifetime;
        private final boolean blackSmoke;
        private int age;

        private ActiveEffect(double x, double y, double z, float scale, int lifetime, boolean blackSmoke) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.scale = scale;
            this.lifetime = lifetime;
            this.blackSmoke = blackSmoke;
        }

        private void tick(ClientLevel level) {
            int count = Math.max(1, Math.round(scale * 1.5f));
            for (int i = 0; i < count; i++) {
                double progress = age / (double) Math.max(1, lifetime);
                double height = 0.4 + progress * (2.0 + scale * 1.4) + RANDOM.nextDouble() * 0.8;
                double radius = 0.25 + progress * (0.6 + scale * 0.25);
                ParticleOptions type = blackSmoke || RANDOM.nextBoolean()
                        ? ParticleTypes.LARGE_SMOKE
                        : ParticleTypes.CAMPFIRE_COSY_SMOKE;
                add(level, type,
                        x + spread(radius), y + height, z + spread(radius),
                        spread(0.018), 0.035 + RANDOM.nextDouble() * 0.035, spread(0.018));
            }
            age++;
        }

        private boolean isDone() {
            return age >= lifetime;
        }
    }

    private static final class PendingSound {
        private final double x;
        private final double y;
        private final double z;
        private final float volume;
        private final float pitch;
        private int delayTicks;
        private boolean played;

        private PendingSound(double x, double y, double z, float volume, float pitch, int delayTicks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.volume = volume;
            this.pitch = pitch;
            this.delayTicks = delayTicks;
        }

        private void tick(ClientLevel level) {
            if (played) return;
            if (delayTicks-- > 0) return;
            play(level, x, y, z, volume, pitch);
            played = true;
        }

        private boolean isDone() {
            return played;
        }
    }
}
