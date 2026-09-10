package com.piranport.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 自动近防炮部件（强化部件槽）。
 * 策划决策/舰装/舰装-自动近防炮系统.md
 *
 * <p>装入舰装核心的强化槽后，每 20 tick 自动检测半径
 * {@link #DETECT_RADIUS} 内的敌对空中实体（飞机/抛射物），命中时造成
 * {@link #DAMAGE} 伤害（不消耗炮弹）。</p>
 *
 * <p>受 H 键总开关控制（《数值/05》定稿），静默窗口（《数值/05》主炮开火后
 * 5 秒定时）期间自动近防炮禁用。</p>
 *
 * <p>不阻塞手持武器的发射 — 与火炮/鱼雷独立开火。</p>
 */
public class AutoCIWSItem extends Item {

    /** 检测半径（针对飞机/抛射物） */
    public static final double DETECT_RADIUS = 16.0;
    /** 每次触发伤害 */
    public static final float DAMAGE = 1.5f;
    /** 触发间隔（每 20 tick = 1 秒） */
    public static final int FIRE_INTERVAL = 20;

    /** 不同口径伤害系数（策划 §自动近防炮 — 数值待填） */
    private final float caliberDamage;

    public AutoCIWSItem(Properties properties, float caliberDamage) {
        super(properties);
        this.caliberDamage = caliberDamage;
    }

    public float getCaliberDamage() {
        return caliberDamage;
    }

    /**
     * 服务端 tick — 由 PlayerTickHandler 调用。
     * <p>简化版：仅在玩家变身状态下、且 H 键自动模式已启用时扫描范围内敌对飞机/抛射物。</p>
     */
    public static void tickAutoCIWS(Player player, boolean autoFireEnabled) {
        if (player == null) return;
        if (!autoFireEnabled) return;
        if (player.level().isClientSide()) return;
        if (player.tickCount % FIRE_INTERVAL != 0) return;

        // AbstractArrow 继承 Projectile 而非 LivingEntity；故此处用 Entity 基类查询
        List<Entity> targets = player.level().getEntitiesOfClass(Entity.class,
                player.getBoundingBox().inflate(DETECT_RADIUS),
                e -> e != player && e.isAlive() && !e.isAlliedTo(player));
        for (Entity t : targets) {
            boolean isArrow = t instanceof net.minecraft.world.entity.projectile.AbstractArrow;
            boolean isAircraft = t instanceof com.piranport.entity.AircraftEntity;
            if (!isArrow && !isAircraft) {
                continue;
            }
            // 仅对飞机（AircraftEntity = LivingEntity 子类）造成伤害
            if (t instanceof LivingEntity living) {
                living.hurt(player.damageSources().mobAttack(player), DAMAGE);
            } else {
                // 抛射物用 discard 代替击杀
                t.discard();
            }
        }
    }
}