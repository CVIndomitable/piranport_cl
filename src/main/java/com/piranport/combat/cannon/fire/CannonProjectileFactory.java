package com.piranport.combat.cannon.fire;

import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.SanshikiPelletEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 炮弹实体工厂，集中保留请求到实体的字段映射。 */
public final class CannonProjectileFactory {
    private CannonProjectileFactory() {}

    public static CannonProjectileEntity create(CannonFireRequest request) {
        return CannonFireService.createProjectile(request);
    }

    /**
     * Create the child pellets for a Type 3 shell at the same projectile boundary as normal shells.
     * The pellet is a different entity because it has its own spread and lifetime, but firing code
     * should still use this factory rather than constructing a projectile entity directly.
     */
    public static SanshikiPelletEntity createSanshikiPellet(Level level, LivingEntity shooter,
            float damage, ItemStack shell) {
        if (level == null) throw new IllegalArgumentException("level must not be null");
        if (shooter == null) throw new IllegalArgumentException("shooter must not be null");
        if (shell == null || shell.isEmpty()) throw new IllegalArgumentException("shell must not be empty");
        return new SanshikiPelletEntity(level, shooter, damage, shell);
    }
}
