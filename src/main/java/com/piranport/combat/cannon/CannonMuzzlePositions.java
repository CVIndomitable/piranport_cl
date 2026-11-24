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
    private static final List<Vec3> FOURTEEN_BARREL_PORT_TIPS = List.of(
            new Vec3(7.49, 3.65, 19.60),
            new Vec3(6.12, 3.65, 19.60),
            new Vec3(5.06, 4.95, 7.55),
            new Vec3(4.33, 7.82, 0.45),
            new Vec3(3.79, 4.95, 7.55),
            new Vec3(3.19, 7.82, 0.45),
            new Vec3(1.63, 6.50, 23.25)
    );

    private CannonMuzzlePositions() {}

    public static List<Vec3> resolve(Player player, ItemStack weapon, int slot, RiggingLoadout loadout) {
        var muzzles = CannonStats.getMuzzlePositions(weapon, player.level());
        RiggingSocket socket = cannonSocket(loadout, slot);
        if (socket == null) {
            Vec3 eye = player.getEyePosition();
            return muzzles.stream().map(m -> eye.add(handheldOffset(m, player.getYRot(), player.getXRot()))).toList();
        }
        var mount = RiggingGeometry.sideMount(socket);
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
            case "single_small_gun" -> new Vec3(-1.5, -2, 16);
            case "chinese_twin_140mm_gun" -> new Vec3(barrel == 0 ? 2.25 : -2.25, -3.5, 17.25);
            case "oto_twin_76mm_rapid_gun" -> barrel == 0
                    ? new Vec3(0, -5.13, 19.68) : new Vec3(0, -1.85, 12.30);
            case "type79_twin_100mm_gun" -> new Vec3(barrel == 0 ? 2 : -2, 1, 17);
            // The modeled barrels are elevated; measure the tip after the element rotation and FIXED.
            case "type79b_twin_100mm_gun" -> new Vec3(barrel == 0 ? 1.6564 : -1.6564, 4.832, 18.3171);
            case "japanese_127mm_twin_gun" ->
                    new Vec3(barrel == 0 ? 3 : -3, 8, 24).scale(RiggingGeometry.COMPACT_TWIN_GUN_MODEL_SCALE);
            case "us_single_5inch_gun" ->
                    new Vec3(0, 8, 21).scale(RiggingGeometry.US_SINGLE_5IN_MODEL_SCALE);
            case "us_twin_5inch_gun" ->
                    new Vec3(barrel == 0 ? 3.5 : -3.5, 8.5, 24).scale(RiggingGeometry.COMPACT_TWIN_GUN_MODEL_SCALE);
            case "us_twin_5inch_dp_gun" ->
                    new Vec3(barrel == 0 ? 4 : -4, 9, 24).scale(RiggingGeometry.COMPACT_TWIN_GUN_MODEL_SCALE);
            case "twin_5inch_l54_dp_gun" -> new Vec3(barrel == 0 ? 1 : -1, 2.25, 15);
            case "japanese_single_140mm_gun" -> new Vec3(0, 2.8, 16.8);
            case "british_single_6inch_gun" -> new Vec3(0, 5.75, 16.75);
            case "japanese_single_152mm_gun" -> new Vec3(0, 5.25, 18);
            case "british_twin_6inch_gun" -> new Vec3(barrel == 0 ? 2.35 : -2.35, 5.05, 18);
            case "japanese_twin_152mm_gun" -> new Vec3(barrel == 0 ? 2.45 : -2.45, 5.55, 18);
            case "british_triple_6inch_gun" -> new Vec3(3.34 * (1 - barrel), 3.90, 23.25);
            case "us_triple_6inch_gun" -> new Vec3(2.55 * (1 - barrel), 5.1, 18);
            case "mk16_triple_6inch_gun_flashless" -> new Vec3(3.35 * (1 - barrel), 5.4, 23);
            case "us_mk16dp_twin_6inch_gun" -> new Vec3(barrel == 0 ? 2.65 : -2.65, 5.3, 23.25);
            case "improved_triple_6inch_gun" -> new Vec3(3.25 * (1 - barrel), 5.4, 23.25);
            case "triple_152mm_dp_gun_46" -> new Vec3(3.45 * (1 - barrel), 5.4, 23.25);
            case "swedish_triple_152mm_gun" -> new Vec3(3.05 * (1 - barrel), 4.95, 23.25);
            case "soviet_triple_180mm_gun" -> new Vec3(2.35 * (1 - barrel), 4.55, 23.25);
            case "soviet_improved_triple_180mm_gun" -> new Vec3(2.95 * (1 - barrel), 4.8, 23.25);
            case "british_twin_8inch_gun" -> new Vec3(barrel == 0 ? 2.8 : -2.8, 4.7, 23.25);
            case "japanese_twin_203mm_gun" -> new Vec3(barrel == 0 ? 2.85 : -2.85, 4.7, 22.75);
            case "german_twin_203mm_gun" -> new Vec3(barrel == 0 ? 2.95 : -2.95, 4.46, 22.75);
            case "german_triple_203mm_gun" -> new Vec3(3.5 * (1 - barrel), 4.75, 23.25);
            case "german_triple_283mm_gun" -> new Vec3(3.75 * (1 - barrel), 4.1, 23.25);
            case "italian_twin_203mm_gun" -> new Vec3(barrel == 0 ? 1.75 : -1.75, 4.65, 23.25);
            case "italian_triple_305mm_gun" -> new Vec3(3.2 * (1 - barrel), 4.95, 23.25);
            case "italian_triple_320mm_gun" -> new Vec3(3.66 * (1 - barrel), 4.65, 23.25);
            case "italian_triple_381mm_gun", "italian_triple_381mm_gun_kai" ->
                    new Vec3(3.75 * (1 - barrel), 3.75, 23.25);
            case "us_twin_16inch_gun", "us_twin_16inch_gun_kai" ->
                    new Vec3(barrel == 0 ? 2.85 : -2.85, 3.15, 23.25);
            case "japanese_twin_356mm_gun" -> new Vec3(barrel == 0 ? 2.22 : -2.22, 2.92, 23.25);
            case "japanese_twin_410mm_gun" -> new Vec3(barrel == 0 ? 2.65 : -2.65, 2.65, 23.25);
            case "us_triple_8inch_gun" -> new Vec3(3.6 * (1 - barrel), 6.7, 23.25);
            case "us_triple_12inch_gun" -> new Vec3(3.55 * (1 - barrel), 4.55, 23.25);
            case "us_triple_14inch_gun", "us_triple_14inch_gun_kai" ->
                    new Vec3(3.35 * (1 - barrel), 3.92, 23.25);
            case "british_quad_14inch_gun" -> new Vec3(3.1 * (1.5 - barrel), 5.15, 23.25);
            case "british_quad_14inch_gun_284" -> new Vec3(3.1 * (1.5 - barrel), 3.65, 23.25);
            case "british_twin_15inch_gun", "british_twin_15inch_gun_kai" ->
                    new Vec3(barrel == 0 ? 2.92 : -2.92, 3.8, 23.25);
            case "german_twin_380mm_gun" -> new Vec3(barrel == 0 ? 2.92 : -2.92, 3.9, 23.25);
            case "german_twin_406mm_gun" -> new Vec3(barrel == 0 ? 2.55 : -2.55, 2.55, 23.25);
            case "british_triple_16inch_gun", "british_triple_16inch_gun_kai" ->
                    new Vec3(3.3 * (1 - barrel), 4.4, 23.25);
            case "us_triple_16inch_gun_mk6" -> new Vec3(3.28 * (1 - barrel), 3.8, 23.25);
            case "us_triple_16inch_gun_mk7" -> new Vec3(3.44 * (1 - barrel), 4.14, 23.25);
            case "german_skc40_triple_420mm_gun" -> new Vec3(3.42 * (1 - barrel), 3.10, 23.25);
            case "british_single_18inch_gun" -> new Vec3(0, 3.74, 23.25);
            case "british_twin_18inch_gun" -> new Vec3(barrel == 0 ? 2.45 : -2.45, 3.72, 23.25);
            case "us_twin_18inch_gun" -> new Vec3(barrel == 0 ? 2.70 : -2.70, 4.20, 23.25);
            case "japanese_triple_460mm_gun" -> new Vec3(3.85 * (1 - barrel), 4.38, 23.25);
            case "soviet_triple_460mm_gun" -> new Vec3(3.72 * (1 - barrel), 3.58, 23.25);
            case "british_twin_20inch_gun" -> new Vec3(barrel == 0 ? 2.45 : -2.45, 3.62, 23.25);
            case "us_twin_20inch_gun" -> new Vec3(barrel == 0 ? 2.88 : -2.88, 3.42, 23.25);
            case "french_quad_380mm_gun" -> new Vec3(switch (barrel) {
                case 0 -> 4.50;
                case 1 -> 1.95;
                case 2 -> -1.95;
                default -> -4.50;
            }, 3.72, 23.25);
            case "fourteen_barrel_gun" -> {
                int last = 2 * FOURTEEN_BARREL_PORT_TIPS.size() - 1;
                if (barrel < 0 || barrel > last) yield legacyModelTip(legacy);
                Vec3 tip = FOURTEEN_BARREL_PORT_TIPS.get(Math.min(barrel, last - barrel));
                yield barrel < FOURTEEN_BARREL_PORT_TIPS.size() ? tip : new Vec3(-tip.x, tip.y, tip.z);
            }
            default -> legacyModelTip(legacy);
        };
    }

    private static Vec3 legacyModelTip(MuzzlePos legacy) {
        // 旧偏移以视点为基准。限制舰装炮口的横向距离，并将炮口置于转座前方。
        return new Vec3(Math.clamp(legacy.x() * 8, -6, 6), -4.5, 16);
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
