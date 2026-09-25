package com.piranport.compat.maid.combat.handlers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.PiranPort;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.combat.cannon.CannonAmmoRules;
import com.piranport.compat.maid.combat.AmmoConsumer;
import com.piranport.compat.maid.combat.WeaponHandler;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.combat.cannon.CannonAim;
import com.piranport.combat.cannon.fire.CannonFireRequest;
import com.piranport.combat.cannon.fire.CannonFireService;
import com.piranport.combat.cannon.fire.CannonProjectileFactory;
import com.piranport.combat.cannon.ammo.AmmoDefinitionService;
import com.piranport.item.ShipCoreItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CannonHandler implements WeaponHandler {
    @Override
    public boolean handles(Item item) {
        return item instanceof ArtilleryItem;
    }

    @Override
    public int cooldownTicks(ItemStack stack) {
        if (stack.getItem() instanceof ArtilleryItem ai) return ai.getCooldownTicks();
        return 30;
    }

    @Override
    public boolean hasAmmo(EntityMaid maid, ItemStack stack) {
        if (!(stack.getItem() instanceof ArtilleryItem ai)) return false;
        Player owner = AmmoConsumer.ownerPlayer(maid);
        if (AmmoConsumer.isFreebie(owner)) return true;
        // 必须凑满一个完整弹夹才放行：fire() 里 loaded 按 Math.min 逐管装填，
        // 弹药不足整夹会照样扣弹但弹数少于 barrels，混装/残缺状态下会一直打残缺齐射。
        // 这里按候选表合计数量判定，兼容「两种弹各半夹、合计刚好够」的合法情形。
        var effectiveData = ai.getEffectiveData(maid.level());
        int barrels = Math.max(1, effectiveData.barrels());
        return AmmoConsumer.countAnyOf(owner, shellsFor(maid.level(), stack)) >= barrels;
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        if (!(stack.getItem() instanceof ArtilleryItem ai)) return;

        // 使用有效数据（考虑配置覆盖）
        var effectiveData = ai.getEffectiveData(maid.level());
        int barrels = Math.max(1, effectiveData.barrels());
        float damage = effectiveData.damage();
        float explosion = effectiveData.explosionPower();
        float velocity = effectiveData.initialSpeed();
        // 根据散布角计算不精确度（简化映射）
        float inaccuracy = effectiveData.dispersion();

        Vec3 origin = maid.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(origin);
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();
        // 射线方向除以长度后 y 必落在 [-1,1]；此处显式夹紧只为防浮点误差让 asin 返回 NaN
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) Math.toDegrees(-Math.asin(clampUnit(aim.y)));

        Player owner = AmmoConsumer.ownerPlayer(maid);
        List<Item> candidates = shellsFor(maid.level(), stack);

        // 先按「副手优先、否则候选表顺序」取偏好弹种，再逐类取弹。
        // 每发都必须记住自己消耗的是哪个弹种：弹种决定 isHE / 近炸引信 / 三式霰弹 / 过穿口径，
        // 此前统一按 preferred 构造导致「只要拿到弹药就恒按 HE 结算」。
        List<Item> shotShells = new ArrayList<>(barrels);
        Item preferred = AmmoConsumer.getPreferredAmmo(owner, candidates);
        if (preferred != null) {
            int n = AmmoConsumer.consumeItem(owner, preferred, barrels);
            for (int i = 0; i < n; i++) shotShells.add(preferred);
        }
        for (Item shell : candidates) {
            if (shotShells.size() >= barrels) break;
            int n = AmmoConsumer.consumeItem(owner, shell, barrels - shotShells.size());
            for (int i = 0; i < n; i++) shotShells.add(shell);
        }

        if (shotShells.isEmpty()) return;

        Level level = maid.level();
        int sourceCaliber = effectiveData.caliber();

        for (Item shell : shotShells) {
            ItemStack shellStack = new ItemStack(shell);
            // 弹种语义与玩家路径对齐：参照 CannonFiring.java:57-59 的推导
            boolean isVT = CannonAmmoRules.isVTShell(shellStack);
            boolean isHE = CannonAmmoRules.isHEShell(shellStack) || isVT;

            // 副本/08 决策：MK23 核炮弹威力 = HE 表值 ×10（写死查表，不走运行时系数）。
            // 玩家路径在 CannonProjectiles.java:84 同样放大；此处按弹逐发计算，
            // 不放在循环外，是因为同一轮齐射可能混装（MK23 只占其中一部分）。
            float shellExplosion = CannonAmmoRules.isMK23Shell(shellStack)
                    && AmmoDefinitionService.find(BuiltInRegistries.ITEM.getKey(shellStack.getItem())).isEmpty()
                    ? explosion * 10f : explosion;

            Vec3 spawnPos = origin;
            CannonFireRequest request = CannonFireService.request(
                    level, maid, stack, shellStack, damage, shellExplosion, velocity,
                    effectiveData.dragCoeff(), effectiveData.gravity(), inaccuracy, inaccuracy,
                    sourceCaliber, isHE, isVT,
                    new CannonAim.DirectAim(target.getBoundingBox().getCenter()), spawnPos);
            if (!CannonFireService.isValid(request)) continue;
            CannonProjectileEntity proj = CannonProjectileFactory.create(request);
            proj.setPos(origin.x, origin.y, origin.z);
            proj.shootFromRotation(maid, pitch, yaw, 0f, velocity, inaccuracy);
            level.addFreshEntity(proj);
        }
    }

    /** 把方向向量的 y 分量夹到 asin 定义域内。 */
    private static double clampUnit(double v) {
        return v < -1.0 ? -1.0 : Math.min(v, 1.0);
    }

    /** 女仆候选弹药来自统一定义注册表，标签仍是最终准入条件。 */

    /** 一次同步日志的静默期，避免每次开火都刷屏。 */
    private static final long TAG_DRIFT_LOG_INTERVAL_TICKS = 600L;
    private static long lastTagDriftLogTick = Long.MIN_VALUE;

    /**
     * 女仆可用弹种 = 玩家路径同源口径标签 ∩ 本表全集，按本表顺序排列。
     * <p>玩家装填走 {@link CannonAmmoRules#matchesCaliber}（口径→标签匹配），此处同样以标签为准，
     * 于是「玩家能装的弹，女仆就能用」，新增弹种只改标签 JSON 即可自动生效，
     * 不会再出现女仆硬编码清单落后于标签的漂移。
     * <p>用候选全集而非遍历标签内容，是因为需在服务端线程外也能安全调用（标签在数据包重载时才绑定），
     * 且遍历标签拿到的是 {@code Holder}，还原成 {@code Item} 需要额外非空判断。
     */
    private static List<Item> shellsFor(Level level, ItemStack weapon) {
        CannonAmmoRules.CaliberFamily want = CannonAmmoRules.familyForWeapon(weapon, level);
        TagKey<Item> tag = switch (want) {
            case SMALL -> ShipCoreItem.SMALL_SHELLS;
            case MEDIUM -> ShipCoreItem.MEDIUM_SHELLS;
            case LARGE -> ShipCoreItem.LARGE_SHELLS;
        };
        List<Item> allowed = new ArrayList<>();
        for (var definition : AmmoDefinitionService.allInOrder()) {
            if (definition.caliberFamily().isEmpty() || definition.caliberFamily().get() != want) continue;
            Item item = BuiltInRegistries.ITEM.get(definition.itemId());
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
            ItemStack probe = new ItemStack(item);
            if (probe.is(tag)) allowed.add(item);
            else logTagDrift(level, tag, item, probe);
        }
        return allowed;
    }

    /** 发现「候选全集」与标签不一致时打一次告警（带上物品 id，便于直接照着补标签 JSON）。 */
    private static void logTagDrift(Level level, TagKey<Item> tag, Item item, ItemStack probe) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        long now = serverLevel.getGameTime();
        if (now - lastTagDriftLogTick < TAG_DRIFT_LOG_INTERVAL_TICKS) return;
        lastTagDriftLogTick = now;
        PiranPort.LOGGER.warn("[女仆火炮] 弹药定义与标签 {} 不一致：物品注册 id {}（当前表现为女仆无法使用该弹）。"
                + "请同步 data/piranport/tags/item/ 下的标签 JSON 与 AmmoDefinitionService。",
                tag.location(), BuiltInRegistries.ITEM.getKey(probe.getItem()));
    }
}
