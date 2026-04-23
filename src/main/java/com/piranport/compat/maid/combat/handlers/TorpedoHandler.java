package com.piranport.compat.maid.combat.handlers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.compat.maid.combat.AmmoConsumer;
import com.piranport.compat.maid.combat.WeaponHandler;
import com.piranport.entity.TorpedoEntity;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public class TorpedoHandler implements WeaponHandler {
    private static final float DEFAULT_SPEED = 0.817f;

    @Override
    public boolean handles(Item item) {
        return item instanceof TorpedoLauncherItem;
    }

    @Override
    public int cooldownTicks(ItemStack stack) {
        return stack.getItem() instanceof TorpedoLauncherItem t ? t.getCooldownTicks() : 100;
    }

    @Override
    public boolean hasAmmo(EntityMaid maid, ItemStack stack) {
        if (!(stack.getItem() instanceof TorpedoLauncherItem launcher)) return false;
        Player owner = AmmoConsumer.ownerPlayer(maid);
        return AmmoConsumer.has(owner, caliberMatcher(launcher.getCaliber()), 1);
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        if (!(stack.getItem() instanceof TorpedoLauncherItem launcher)) return;
        Player owner = AmmoConsumer.ownerPlayer(maid);
        int tubes = Math.max(1, launcher.getTubeCount());
        int caliber = launcher.getCaliber();
        int loaded = AmmoConsumer.consume(owner, caliberMatcher(caliber), tubes);
        if (loaded <= 0) return;

        Level level = maid.level();
        TorpedoItem reference = findFirstTorpedo(owner, caliber);
        float damage = reference != null ? reference.getDamage() : 27f;
        float speed = reference != null ? reference.getSpeed() : DEFAULT_SPEED;
        int lifetime = reference != null ? reference.getLifetimeTicks() : 360;

        Vec3 origin = maid.position().add(0, 0.2, 0);
        Vec3 aim = target.position().subtract(origin);
        aim = new Vec3(aim.x, 0, aim.z);
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();
        float baseYaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));

        float[] offsets = spreadOffsets(loaded);
        for (int i = 0; i < loaded; i++) {
            float yaw = baseYaw + offsets[i];
            TorpedoEntity torp = new TorpedoEntity(level, maid, caliber);
            torp.setDamage(damage);
            torp.setSpeed(speed);
            torp.setLifetime(lifetime);
            torp.setPos(origin.x, origin.y, origin.z);
            torp.shootFromRotation(maid, 0f, yaw, 0f, speed, 0.5f);
            level.addFreshEntity(torp);
        }
    }

    private static Predicate<ItemStack> caliberMatcher(int caliber) {
        return s -> !s.isEmpty()
                && s.getItem() instanceof TorpedoItem ti
                && ti.getCaliber() == caliber;
    }

    private static TorpedoItem findFirstTorpedo(Player player, int caliber) {
        if (player == null) return null;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                return ti;
            }
        }
        return null;
    }

    private static float[] spreadOffsets(int tubes) {
        return switch (tubes) {
            case 1 -> new float[]{0f};
            case 2 -> new float[]{-3f, 3f};
            case 3 -> new float[]{-4f, 0f, 4f};
            case 4 -> new float[]{-6f, -2f, 2f, 6f};
            default -> {
                float[] arr = new float[tubes];
                float step = 8f / Math.max(1, tubes - 1);
                for (int i = 0; i < tubes; i++) arr[i] = -4f + i * step;
                yield arr;
            }
        };
    }
}
