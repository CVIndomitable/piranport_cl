package com.piranport.item;

import com.piranport.config.ModEquipmentConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 独立灭火器物品（消耗品/02-灭火器设计）。
 * <p>右键使用：清除身上所有 HARMFUL 效果并灭火；冷却走本物品独立终端参数
 * {@code global.fire_extinguisher.use_cooldown}（默认 2400 tick = 120 秒），
 * 与损管的 {@code global.damage_control.use_cooldown} 是两套参数，不共用。
 * 与损管区别：不消耗"强化槽"概念，背包槽位消耗，配方原料不同。</p>
 *
 * <p>与损管共用同一套「超载效果保留」白名单：挖掘疲劳、虚弱、迟缓是舰装过载的
 * 安全警示，灭火器只清除其余 debuff（《舰装/03-武器弹药与消耗品》）。</p>
 */
public class FireExtinguisherItem extends Item {

    /** 超载相关效果：不得被灭火器清除。2026-10-19 起 150% 档由中毒改为迟缓。 */
    private static final Set<ResourceKey<MobEffect>> OVERLOAD_EFFECT_KEYS = new HashSet<>();

    static {
        OVERLOAD_EFFECT_KEYS.add(BuiltInRegistries.MOB_EFFECT
                .getResourceKey(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN.value()).orElseThrow());
        OVERLOAD_EFFECT_KEYS.add(BuiltInRegistries.MOB_EFFECT
                .getResourceKey(net.minecraft.world.effect.MobEffects.WEAKNESS.value()).orElseThrow());
        OVERLOAD_EFFECT_KEYS.add(BuiltInRegistries.MOB_EFFECT
                .getResourceKey(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN.value()).orElseThrow());
    }

    public FireExtinguisherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 清除所有 HARMFUL 效果，但保留超载相关效果（挖掘疲劳/虚弱/迟缓）
            List<MobEffectInstance> toRemove = new ArrayList<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) continue;
                var key = effect.getEffect().unwrapKey();
                if (key.isPresent() && OVERLOAD_EFFECT_KEYS.contains(key.get())) continue;
                toRemove.add(effect);
            }
            for (MobEffectInstance effect : toRemove) {
                player.removeEffect(effect.getEffect());
            }

            // 灭火
            player.clearFire();
        }

        // 音效
        level.playSound(player, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 1.0F, 1.0F);

        // 冷却
        player.getCooldowns().addCooldown(this, ModEquipmentConfig.FIRE_EXTINGUISHER_USE_COOLDOWN.get());

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
