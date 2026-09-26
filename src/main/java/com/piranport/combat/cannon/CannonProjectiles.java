package com.piranport.combat.cannon;

import com.piranport.combat.cannon.fire.CannonFireService;
import com.piranport.combat.cannon.fire.CannonFireRequest;
import com.piranport.combat.cannon.ammo.AmmoDefinitionService;
import com.piranport.network.ShakeEffectPayload;
import com.piranport.server.ScopingManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

import static com.piranport.combat.cannon.CannonStats.*;

/** 玩家只适配瞄准与炮口；弹种、限额、实体及散布由共享发射边界处理。 */
final class CannonProjectiles {
    private CannonProjectiles() {}

    static CannonFireService.Result fireCannonSalvo(Level level, Player player, ItemStack weapon,
            ItemStack shell, int barrels, boolean type3, boolean vt, boolean he, CannonAim aim) {
        var muzzles = getMuzzlePositions(weapon, level);
        Vec3 aimOrigin = player.getEyePosition().add(CannonAiming.rotateMuzzleByPlayerView(player, muzzles.get((muzzles.size() - 1) / 2)));
        float velocity = getProjectileVelocity(weapon, level);
        float horizontal = getHorizontalSpread(weapon, level);
        float vertical = getVerticalSpread(weapon, level);
        if (!ScopingManager.isScoping(player)) {
            horizontal *= 1.1f;
            vertical *= 1.1f;
        }
        float explosion = getExplosionPower(weapon, level);
        if (CannonAmmoRules.isMK23Shell(shell)
                && AmmoDefinitionService.find(BuiltInRegistries.ITEM.getKey(shell.getItem())).isEmpty()) explosion *= 10f;
        int caliber = ((com.piranport.artillery.ArtilleryItem) weapon.getItem()).getEffectiveData(level).caliber();
        List<CannonFireService.Shot> shots = new ArrayList<>();
        for (int barrel = 0; barrel < barrels; barrel++) {
            Vec3 spawn = player.getEyePosition().add(CannonAiming.rotateMuzzleByPlayerView(player, muzzles.get(barrel % muzzles.size())));
            Vec3 direction = CannonAiming.resolveDirection(player, weapon, velocity, aim, aimOrigin, spawn);
            try {
                CannonFireRequest request = CannonFireService.request(level, player, weapon, shell,
                        getGunDamage(weapon, level), explosion, velocity, getProjectileDrag(weapon, level),
                        getProjectileGravity(weapon, level), horizontal, vertical, caliber, he, vt, aim, spawn);
                shots.add(new CannonFireService.Shot(request, direction, type3));
            } catch (IllegalArgumentException invalid) {
                return new CannonFireService.Result(0, 0, false);
            }
        }
        if (!CannonFireService.validatePlan(shots).isEmpty()) return new CannonFireService.Result(0, 0, false);
        CannonFireService.Result result = CannonFireService.emit(shots);
        if (result.shots() == 0) return result;
        if (!player.getAbilities().instabuild && weapon.isDamageableItem()
                && weapon.getDamageValue() < getCannonDurability(weapon, level) - 1) {
            weapon.setDamageValue(weapon.getDamageValue() + 1);
        }
        if (level instanceof ServerLevel serverLevel) {
            for (int barrel = 0; barrel < result.shots(); barrel++) {
                Vec3 spawn = shots.get(barrel).request().spawnPosition();
                Vec3 flame = spawn.add(player.getLookAngle().scale(.3));
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, flame.x, flame.y, flame.z, 3, .2, .2, .2, .05);
                serverLevel.sendParticles(ParticleTypes.CLOUD, flame.x, flame.y, flame.z, 2, .3, .15, .3, .01);
                serverLevel.sendParticles(ParticleTypes.LAVA, flame.x, flame.y, flame.z, 1, .1, .1, .1, 0);
            }
        }
        CannonSounds.playCannonFireSound(level, player, weapon);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ShakeEffectPayload(isSmallCaliber(weapon, level) ? .3f : .6f, 6));
        }
        return result;
    }
}
