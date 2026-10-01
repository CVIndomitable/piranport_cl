package com.piranport.dungeon.key;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.PiranPort;
import com.piranport.registry.ModAttachmentTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 战利品条件 {@code piranport:entered_dungeon}：开箱玩家是否带「已进入过副本」标记。
 *
 * <p>用于书台遗迹箱子（策划决策/副本/17 §二）：{@code "entered": false} 发 1-1 钥匙，
 * {@code "entered": true} 发金猫猫钥匙。没有开箱实体（如漏斗触发）时按「未进入」处理。
 */
public record EnteredDungeonCondition(boolean entered) implements LootItemCondition {

    public static final MapCodec<EnteredDungeonCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("entered").forGetter(EnteredDungeonCondition::entered)
    ).apply(i, EnteredDungeonCondition::new));

    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, PiranPort.MOD_ID);

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> TYPE =
            LOOT_CONDITIONS.register("entered_dungeon", () -> new LootItemConditionType(CODEC));

    @Override
    public LootItemConditionType getType() {
        return TYPE.get();
    }

    @Override
    public boolean test(LootContext context) {
        Entity opener = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        boolean has = opener instanceof Player player && hasEntered(player);
        return has == entered;
    }

    public static boolean hasEntered(Player player) {
        return Boolean.TRUE.equals(player.getData(ModAttachmentTypes.ENTERED_DUNGEON));
    }

    public static void markEntered(Player player) {
        if (!hasEntered(player)) player.setData(ModAttachmentTypes.ENTERED_DUNGEON, true);
    }
}
