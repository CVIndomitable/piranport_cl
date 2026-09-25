package com.piranport.combat.cannon.fire;

import com.piranport.combat.cannon.CannonAim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * 一次炮弹发射所需的不可变输入快照。
 *
 * <p>请求只描述发射边界，不负责扣耐久、消耗弹药或播放表现。所有 ItemStack
 * 都在构造时复制，避免调用方在排队或异步处理期间改变请求语义。</p>
 */
public record CannonFireRequest(
        Level level,
        LivingEntity shooter,
        ItemStack weapon,
        ItemStack shell,
        float damage,
        float explosionPower,
        float velocity,
        float drag,
        float gravity,
        float horizontalSpread,
        float verticalSpread,
        int sourceCaliber,
        boolean highExplosive,
        boolean proximityFuse,
        CannonAim aim,
        Vec3 spawnPosition) {

    public CannonFireRequest {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(shooter, "shooter");
        weapon = copyNonEmpty(weapon, "weapon");
        shell = copyNonEmpty(shell, "shell");
        Objects.requireNonNull(aim, "aim");
        Objects.requireNonNull(spawnPosition, "spawnPosition");
        if (!isFiniteNonNegative(damage)) throw new IllegalArgumentException("damage must be finite and non-negative");
        if (!isFiniteNonNegative(explosionPower)) throw new IllegalArgumentException("explosionPower must be finite and non-negative");
        if (!Float.isFinite(velocity) || velocity <= 0) throw new IllegalArgumentException("velocity must be positive");
        if (!Float.isFinite(drag) || drag <= 0) throw new IllegalArgumentException("drag must be positive");
        if (!Float.isFinite(gravity) || gravity < 0) throw new IllegalArgumentException("gravity must be finite and non-negative");
        if (!isFiniteNonNegative(horizontalSpread) || !isFiniteNonNegative(verticalSpread)) {
            throw new IllegalArgumentException("spread must be finite and non-negative");
        }
        if (sourceCaliber < 0) throw new IllegalArgumentException("sourceCaliber must be non-negative");
        if (!isFinite(spawnPosition)) throw new IllegalArgumentException("spawnPosition must be finite");
    }

    @Override public ItemStack weapon() { return weapon.copy(); }
    @Override public ItemStack shell() { return shell.copy(); }

    private static ItemStack copyNonEmpty(ItemStack stack, String name) {
        Objects.requireNonNull(stack, name);
        if (stack.isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
        return stack.copy();
    }

    private static boolean isFiniteNonNegative(float value) {
        return Float.isFinite(value) && value >= 0;
    }

    private static boolean isFinite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
}
