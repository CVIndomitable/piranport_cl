package com.piranport.client;

import com.piranport.network.CannonImpactEffectPayload;
import com.piranport.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 客户端远距炮弹命中特效。 */
public final class CannonImpactEffects {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<ActiveEffect> ACTIVE_EFFECTS = new CopyOnWriteArrayList<>();

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
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            ACTIVE_EFFECTS.clear();
            return;
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
        play(level, x, y, z, Math.min(2.4f, 1.0f + scale * 0.35f), 0.68f + RANDOM.nextFloat() * 0.08f);
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
        play(level, x, y, z, Math.min(1.2f, 0.45f + scale * 0.16f), 0.78f + RANDOM.nextFloat() * 0.08f);
    }

    private static void spawnVt(ClientLevel level, double x, double y, double z, float scale) {
        spawnSmokeColumn(level, x, y, z, scale * 0.85f, Math.round(12 + scale * 4), true);
        ACTIVE_EFFECTS.add(new ActiveEffect(x, y, z, scale * 0.75f, 36, true));
        play(level, x, y, z, Math.min(1.8f, 0.75f + scale * 0.22f), 0.58f + RANDOM.nextFloat() * 0.07f);
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

    private static void play(ClientLevel level, double x, double y, double z, float volume, float pitch) {
        level.playLocalSound(x, y, z, ModSounds.CANNON_EXPLOSION.get(), SoundSource.PLAYERS, volume, pitch, false);
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
}
