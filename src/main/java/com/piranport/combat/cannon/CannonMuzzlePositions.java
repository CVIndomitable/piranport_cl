package com.piranport.combat.cannon;

import com.piranport.artillery.config.MuzzlePos;
import com.piranport.rigging.RiggingCategory;
import com.piranport.rigging.RiggingGeometry;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingSocket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.IntStream;

/** Physical projectile origins. Camera tweaks and interpolated render poses never move a server shot. */
public final class CannonMuzzlePositions {
    private static final double PLAYER_MODEL_SCALE = 0.9375;
    private static final double PLAYER_MODEL_LIFT = 1.501;

    private CannonMuzzlePositions() {}

    public static List<Vec3> resolve(Player player, ItemStack weapon, int slot, RiggingLoadout loadout) {
        var muzzles = CannonStats.getMuzzlePositions(weapon, player.level());
        RiggingSocket socket = cannonSocket(loadout, slot);
        if (socket == null) {
            Vec3 eye = player.getEyePosition();
            return muzzles.stream().map(m -> eye.add(handheldOffset(m, player.getYRot(), player.getXRot()))).toList();
        }
        var mount = RiggingGeometry.sideMount(socket, loadout.hasAircraft());
        var itemId = BuiltInRegistries.ITEM.getKey(weapon.getItem());
        String model = itemId.getNamespace().equals("piranport") ? itemId.getPath() : "";
        Vec3 feet = player.position();
        Vec3 eye = player.getEyePosition();
        float yaw = player.yBodyRot;
        float scale = player.getScale();
        boolean crouching = player.isCrouching();
        return IntStream.range(0, muzzles.size())
                .mapToObj(i -> worldMuzzle(feet, yaw, scale, crouching, mount, modelTip(model, i, muzzles.get(i))))
                .map(tip -> clipToMuzzle(player, eye, tip)).toList();
    }

    public static Vec3 reference(List<Vec3> muzzles) {
        return muzzles.get((muzzles.size() - 1) / 2);
    }

    private static Vec3 clipToMuzzle(Player player, Vec3 eye, Vec3 muzzle) {
        var obstruction = player.level().clip(new ClipContext(eye, muzzle, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return obstruction.getType() == HitResult.Type.MISS ? muzzle : beforeObstruction(eye, obstruction.getLocation());
    }

    static Vec3 beforeObstruction(Vec3 eye, Vec3 hit) {
        Vec3 toHit = hit.subtract(eye);
        return eye.add(toHit.normalize().scale(Math.max(0.0, toHit.length() - 0.05)));
    }

    static RiggingSocket cannonSocket(RiggingLoadout loadout, int slot) {
        if (slot < 0 || loadout.isEmpty()) return null;
        for (var mount : loadout.mounts()) {
            if (mount.sourceSlot() == slot && mount.category() == RiggingCategory.CANNON) return mount.socket();
        }
        return null;
    }

    /** Tip after the item's FIXED transform, in model pixels (left, up, forward). */
    static Vec3 modelTip(String model, int barrel, MuzzlePos legacy) {
        return switch (model) {
            case "chinese_twin_140mm_gun" -> new Vec3(barrel == 0 ? 2.25 : -2.25, -3.5, 17.25);
            case "oto_twin_76mm_rapid_gun" -> barrel == 0
                    ? new Vec3(0, -5.13, 19.68) : new Vec3(0, -1.85, 12.30);
            case "british_triple_6inch_gun" -> new Vec3(1.8 * (1 - barrel), -5.05, 16.5);
            // Legacy offsets were authored in eye space. Keep barrel order/spacing, but bound
            // it to the mounted turret and place the group ahead of its center.
            default -> new Vec3(Math.clamp(legacy.x() * 8, -6, 6), -4.5, 16);
        };
    }

    static Vec3 worldMuzzle(Vec3 feet, float bodyYaw, float entityScale, boolean crouching,
                            RiggingGeometry.SideMount mount, Vec3 modelTip) {
        double scale = RiggingGeometry.SIDE_ITEM_SCALE;
        Vec3 local = new Vec3((mount.left() + modelTip.x * scale) / 16,
                -RiggingGeometry.HULL_DOWN + (-RiggingGeometry.SideMount.DOWN + modelTip.y * scale) / 16,
                -RiggingGeometry.HULL_BACK + (mount.forward() + modelTip.z * scale) / 16);
        // The vanilla standing body pivot is at the top of the torso. Sneaking adds a
        // body tilt (0.5 rad), a 3.2 px pivot drop and a 2 px PlayerRenderer offset.
        if (crouching) local = local.xRot(-0.5f).add(0, -3.2 / 16, 0);
        local = local.add(0, PLAYER_MODEL_LIFT, 0).scale(PLAYER_MODEL_SCALE);
        if (crouching) local = local.add(0, -2.0 / 16, 0);
        return feet.add(local.scale(entityScale).yRot((float) -Math.toRadians(bodyYaw)));
    }

    private static Vec3 handheldOffset(MuzzlePos muzzle, float yaw, float pitch) {
        // Retain the old origin for non-mounted weapons (including overflow inventory slots).
        double yawRad = Math.toRadians(yaw), pitchRad = Math.toRadians(pitch);
        double cosYaw = Math.cos(yawRad), sinYaw = Math.sin(yawRad), cosPitch = Math.cos(pitchRad);
        return new Vec3(muzzle.x() * cosYaw - muzzle.z() * sinYaw,
                muzzle.y() * cosPitch + muzzle.z() * Math.sin(pitchRad),
                muzzle.x() * sinYaw + muzzle.z() * cosYaw * cosPitch);
    }
}
