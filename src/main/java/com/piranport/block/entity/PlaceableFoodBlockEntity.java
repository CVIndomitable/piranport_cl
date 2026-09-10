package com.piranport.block.entity;

import com.piranport.component.PlaceableInfo;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PlaceableFoodBlockEntity extends BlockEntity {
    /**
     * 依据：策划决策/食物/09-食物方块饱食度加成.md
     * <p>方块版本每口饱食度 = ceil(总饱食度 × bonusMultiplier / 可使用次数)</p>
     * <p>默认 bonusMultiplier = 1.5x；可通过物品数据组件 PLACEABLE_INFO.bonusMultiplier 覆盖</p>
     */
    private float bonusMultiplier = PlaceableInfo.DEFAULT_BONUS;

    private ResourceLocation foodItemId = ResourceLocation.withDefaultNamespace("air");
    private int remainingServings = 0;
    private int totalServings = 1;

    public PlaceableFoodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.PLACEABLE_FOOD.get(), pos, state);
    }

    /**
     * 初始化（兼容旧调用）— bonusMultiplier 取默认 1.5x。
     */
    public void initialize(ResourceLocation id, int servings) {
        initialize(id, servings, PlaceableInfo.DEFAULT_BONUS);
    }

    /**
     * 初始化（带 bonusMultiplier）。策划可在物品 JSON/数据中指定。
     */
    public void initialize(ResourceLocation id, int servings, float bonus) {
        this.foodItemId = id;
        this.remainingServings = servings;
        this.totalServings = servings;
        this.bonusMultiplier = bonus;
    }

    /** 运行时探测物品的 PlaceableInfo 并回填 bonusMultiplier。 */
    public void applyPlaceableInfo(@Nullable PlaceableInfo info) {
        if (info != null && info.bonusMultiplier() > 0f) {
            this.bonusMultiplier = info.bonusMultiplier();
        }
    }

    public ResourceLocation getFoodItemId() { return foodItemId; }
    public int getRemainingServings() { return remainingServings; }
    public boolean isEmpty() { return remainingServings <= 0; }
    public float getBonusMultiplier() { return bonusMultiplier; }

    public void eat(Player player) {
        if (remainingServings <= 0 || level == null) return;

        Item foodItem = BuiltInRegistries.ITEM.get(foodItemId);
        if (foodItem == Items.AIR) return;

        FoodProperties food = new ItemStack(foodItem).getFoodProperties(player);
        if (food == null) return;

        // Cumulative allocation — guarantees Σ(bites) == original food value (×bonusMultiplier).
        int bitesDone = totalServings - remainingServings;
        float totalBonusNutrition = food.nutrition() * bonusMultiplier;
        float totalBonusSaturation = food.saturation() * bonusMultiplier;
        int nutritionPerBite = (int) ((long) totalBonusNutrition * (bitesDone + 1) / totalServings)
                - (int) ((long) totalBonusNutrition * bitesDone / totalServings);
        float satModPerBite = totalBonusSaturation * (bitesDone + 1) / totalServings
                - totalBonusSaturation * bitesDone / totalServings;
        player.getFoodData().eat(nutritionPerBite, satModPerBite);

        // 依据：策划决策/食物/09-食物方块饱食度加成.md
        // "概率性效果每口独立判定（如鲱鱼罐头的凋零）保持概率不变"
        boolean isLastBite = (remainingServings == 1);
        if (isLastBite) {
            // 最后一口仍按原版语义触发完整 Buff（含非概率的固定 Buff）
            for (FoodProperties.PossibleEffect pe : food.effects()) {
                MobEffectInstance orig = pe.effect();
                if (player.getRandom().nextFloat() < pe.probability()) {
                    int perBiteDuration = Math.max(1,
                            (orig.getDuration() + totalServings - 1) / totalServings);
                    player.addEffect(new MobEffectInstance(orig.getEffect(),
                            perBiteDuration, orig.getAmplifier()));
                }
            }
        } else {
            // 非最后一口：仅触发概率型效果，时长均分
            for (FoodProperties.PossibleEffect pe : food.effects()) {
                MobEffectInstance orig = pe.effect();
                if (pe.probability() < 1.0f
                        && player.getRandom().nextFloat() < pe.probability()) {
                    int perBiteDuration = Math.max(1,
                            (orig.getDuration() + totalServings - 1) / totalServings);
                    player.addEffect(new MobEffectInstance(orig.getEffect(),
                            perBiteDuration, orig.getAmplifier()));
                }
            }
        }

        remainingServings--;
        if (remainingServings <= 0) {
            // Drop bowl if bowl container
            if (getBlockState().is(ModBlocks.BOWL_FOOD.get())) {
                Block.popResource(level, worldPosition, new ItemStack(Items.BOWL));
            }
            level.removeBlock(worldPosition, false);
        } else {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("foodItemId", foodItemId.toString());
        tag.putInt("remainingServings", remainingServings);
        tag.putInt("totalServings", totalServings);
        tag.putFloat("BonusMultiplier", bonusMultiplier);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("foodItemId")) {
            foodItemId = ResourceLocation.parse(tag.getString("foodItemId"));
        }
        remainingServings = tag.getInt("remainingServings");
        totalServings = tag.getInt("totalServings");
        if (tag.contains("BonusMultiplier")) {
            bonusMultiplier = tag.getFloat("BonusMultiplier");
        }
    }
}
