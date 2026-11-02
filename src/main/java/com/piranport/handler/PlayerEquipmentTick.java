package com.piranport.handler;

import com.piranport.item.ExperienceShellItem;
import com.piranport.item.FootballArmorItem;
import com.piranport.item.KirinHeadbandItem;
import com.piranport.registry.ModMobEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * 装备被动与潜艇效果；由入口分别在变身检查前、变身确认后调用。
 * 仅在服务端主线程执行，不独立订阅事件。
 */
final class PlayerEquipmentTick {
    private PlayerEquipmentTick() {}

    /** 麒麟头巾隐身 + 足球套装/经验炮弹经验加成 */
    static void tickEquipmentPassives(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof KirinHeadbandItem) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 25, 0, false, false, true));
        }
        boolean hasFootball = false;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof FootballArmorItem) {
                hasFootball = true;
                break;
            }
        }
        boolean hasExperienceShell = ExperienceShellItem.hasInInventory(player);
        if (hasFootball || hasExperienceShell) {
            player.addEffect(new MobEffectInstance(ModMobEffects.EXPERIENCE_BOOST, 25, 0, false, false, true));
        } else {
            player.removeEffect(ModMobEffects.EXPERIENCE_BOOST);
        }
    }

    /** 潜艇效果：无限水下呼吸 + 水下隐身 + 深海夜视（仅在效果快过期时刷新，避免每tick发包） */
    static void tickSubmarineEffects(Player player, boolean isSubmarine) {
        if (isSubmarine) {
            MobEffectInstance waterBreathing = player.getEffect(MobEffects.WATER_BREATHING);
            if (waterBreathing == null || waterBreathing.getDuration() <= 100) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, false, true));
            }
            if (player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
                MobEffectInstance invis = player.getEffect(MobEffects.INVISIBILITY);
                if (invis == null || invis.getDuration() <= 10) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
                }
            }
            // 深海夜视：仅在深水区（Y < 50）给予
            if (player.isInWater() && player.getY() < 50) {
                MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
                if (nightVision == null || nightVision.getDuration() <= 60) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false, true));
                }
            }
        }
    }
}
