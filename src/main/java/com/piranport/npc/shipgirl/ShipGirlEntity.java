package com.piranport.npc.shipgirl;

import com.piranport.advancement.ModAdvancements;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.registry.ModItems;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * Friendly ship girl NPC. Can be interacted with for basic status dialog.
 * Optionally fights deep ocean enemies when combat AI is enabled.
 */
public class ShipGirlEntity extends PathfinderMob implements Merchant {
    private static final EntityDataAccessor<Integer> DATA_SKIN_VARIANT =
            SynchedEntityData.defineId(ShipGirlEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RAPPORT =
            SynchedEntityData.defineId(ShipGirlEntity.class, EntityDataSerializers.INT);
    private static final int FIRST_SHIPGIRL_SKIN = 4;
    private static final int LAST_SHIPGIRL_SKIN = 23;
    public static final int KITCHEN_GODDESS_VARIANT = 9857;
    private static final int[] DEFAULT_SKIN_VARIANTS = {
            4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
            KITCHEN_GODDESS_VARIANT
    };
    private static final int MAX_RAPPORT = 100;
    private static final int BRANCH_RECON = 1;
    private static final int BRANCH_MAINTENANCE = 1 << 1;
    private static final int BRANCH_DRILL = 1 << 2;
    private static final int BRANCH_AIR_COVER = 1 << 3;
    private static final int BRANCH_SHARD_RESEARCH = 1 << 4;
    private static final int BRANCH_ALLIANCE_SUPPLY = 1 << 5;
    private static final int[] BRANCH_FLAGS = {
            BRANCH_RECON,
            BRANCH_MAINTENANCE,
            BRANCH_DRILL,
            BRANCH_AIR_COVER,
            BRANCH_SHARD_RESEARCH,
            BRANCH_ALLIANCE_SUPPLY
    };

    private enum OrderMode {
        PATROL("message.piranport.ship_girl_order_mode_patrol"),
        FOLLOW("message.piranport.ship_girl_order_mode_follow"),
        HOLD_POSITION("message.piranport.ship_girl_order_mode_hold");

        private final String translationKey;

        OrderMode(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private enum QuestStage {
        INTEL("message.piranport.ship_girl_quest_stage_intel"),
        PREPARATION("message.piranport.ship_girl_quest_stage_preparation"),
        CALIBRATION("message.piranport.ship_girl_quest_stage_calibration"),
        COMPLETE("message.piranport.ship_girl_quest_stage_complete");

        private final String translationKey;

        QuestStage(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private enum FollowUpStage {
        FLAGSHIP_DEBRIEF("message.piranport.ship_girl_followup_stage_flagship"),
        SHARD_STABILIZATION("message.piranport.ship_girl_followup_stage_shard"),
        ALLIANCE_CONTACT("message.piranport.ship_girl_followup_stage_alliance"),
        COMPLETE("message.piranport.ship_girl_followup_stage_complete");

        private final String translationKey;

        FollowUpStage(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private static final String[] DIALOGUE_KEYS = {
            "message.piranport.ship_girl_greet",
            "message.piranport.ship_girl_patrol",
            "message.piranport.ship_girl_abyssal",
            "message.piranport.ship_girl_supplies"
    };

    private static final String[] INTEL_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_intel_1",
            "message.piranport.ship_girl_dialogue_intel_2"
    };

    private static final String[] PREPARATION_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_preparation_1",
            "message.piranport.ship_girl_dialogue_preparation_2"
    };

    private static final String[] CALIBRATION_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_calibration_1",
            "message.piranport.ship_girl_dialogue_calibration_2"
    };

    private static final String[] COMPLETE_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_complete_1",
            "message.piranport.ship_girl_dialogue_complete_2"
    };

    private static final String[] FOLLOWUP_FLAGSHIP_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_followup_flagship_1",
            "message.piranport.ship_girl_dialogue_followup_flagship_2",
            "message.piranport.ship_girl_dialogue_followup_flagship_3"
    };

    private static final String[] FOLLOWUP_SHARD_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_followup_shard_1",
            "message.piranport.ship_girl_dialogue_followup_shard_2",
            "message.piranport.ship_girl_dialogue_followup_shard_3"
    };

    private static final String[] FOLLOWUP_ALLIANCE_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_followup_alliance_1",
            "message.piranport.ship_girl_dialogue_followup_alliance_2",
            "message.piranport.ship_girl_dialogue_followup_alliance_3"
    };

    private static final String[] BRANCH_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_branch_1",
            "message.piranport.ship_girl_dialogue_branch_2",
            "message.piranport.ship_girl_dialogue_branch_3",
            "message.piranport.ship_girl_dialogue_branch_4"
    };

    private static final String[] BRANCH_COMPLETE_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_branch_complete_1",
            "message.piranport.ship_girl_dialogue_branch_complete_2"
    };

    private static final String[] HIGH_RAPPORT_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_rapport_1",
            "message.piranport.ship_girl_dialogue_rapport_2",
            "message.piranport.ship_girl_dialogue_rapport_3"
    };

    private static final String[] FACTION_J_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_faction_j_1",
            "message.piranport.ship_girl_dialogue_faction_j_2",
            "message.piranport.ship_girl_dialogue_faction_j_3"
    };

    private static final String[] FACTION_C_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_faction_c_1",
            "message.piranport.ship_girl_dialogue_faction_c_2",
            "message.piranport.ship_girl_dialogue_faction_c_3"
    };

    private static final String[] FACTION_G_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_faction_g_1",
            "message.piranport.ship_girl_dialogue_faction_g_2",
            "message.piranport.ship_girl_dialogue_faction_g_3"
    };

    private static final String[] FACTION_I_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_faction_i_1",
            "message.piranport.ship_girl_dialogue_faction_i_2",
            "message.piranport.ship_girl_dialogue_faction_i_3"
    };

    private static final String[] FACTION_E_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_faction_e_1",
            "message.piranport.ship_girl_dialogue_faction_e_2",
            "message.piranport.ship_girl_dialogue_faction_e_3"
    };

    private static final String[] FACTION_F_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_faction_f_1",
            "message.piranport.ship_girl_dialogue_faction_f_2",
            "message.piranport.ship_girl_dialogue_faction_f_3"
    };

    private static final String[] FACTION_U_DIALOGUE_KEYS = {
            "message.piranport.ship_girl_dialogue_faction_u_1",
            "message.piranport.ship_girl_dialogue_faction_u_2",
            "message.piranport.ship_girl_dialogue_faction_u_3"
    };

    private boolean combatAiEnabled = true;
    private OrderMode orderMode = OrderMode.PATROL;
    private UUID orderPlayerUuid = null;
    private BlockPos orderHomePos = null;
    private Player tradingPlayer = null;
    private MerchantOffers offers = null;
    private int tradeXp = 0;
    private QuestStage questStage = QuestStage.INTEL;
    private FollowUpStage followUpStage = FollowUpStage.FLAGSHIP_DEBRIEF;
    private int rapport = 0;
    private int branchMask = 0;

    public ShipGirlEntity(EntityType<? extends ShipGirlEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN_VARIANT, 0);
        builder.define(DATA_RAPPORT, 0);
    }

    @Override
    protected void registerGoals() {
        // Movement
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new FollowOrderGoal(this, 1.05, 4.0f, 12.0f));
        this.goalSelector.addGoal(3, new HoldPositionGoal(this, 0.9));
        this.goalSelector.addGoal(5, new PatrolStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Target deep ocean enemies — runtime-gated by combatAiEnabled
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false,
                e -> e instanceof AbstractDeepOceanEntity) {
            @Override
            public boolean canUse() {
                return ((ShipGirlEntity) this.mob).isCombatAiEnabled() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return ((ShipGirlEntity) this.mob).isCombatAiEnabled() && super.canContinueToUse();
            }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && getSkinVariant() <= 0) {
            setSkinVariant(pickDefaultSkinVariant());
        }
    }

    public int getSkinVariant() {
        return entityData.get(DATA_SKIN_VARIANT);
    }

    public int getRapport() {
        return entityData.get(DATA_RAPPORT);
    }

    public void setSkinVariant(int skinVariant) {
        entityData.set(DATA_SKIN_VARIANT, clampSkinVariant(skinVariant));
    }

    private int pickDefaultSkinVariant() {
        return DEFAULT_SKIN_VARIANTS[Math.floorMod(getUUID().hashCode(), DEFAULT_SKIN_VARIANTS.length)];
    }

    private static int clampSkinVariant(int skinVariant) {
        if (skinVariant == KITCHEN_GODDESS_VARIANT) {
            return skinVariant;
        }
        if (skinVariant < FIRST_SHIPGIRL_SKIN || skinVariant > LAST_SHIPGIRL_SKIN) {
            return FIRST_SHIPGIRL_SKIN;
        }
        return skinVariant;
    }

    public boolean isCombatAiEnabled() {
        return combatAiEnabled;
    }

    public void setCombatAiEnabled(boolean enabled) {
        this.combatAiEnabled = enabled;
    }

    private boolean isFollowingOrder() {
        return orderMode == OrderMode.FOLLOW;
    }

    private boolean isHoldingPositionOrder() {
        return orderMode == OrderMode.HOLD_POSITION;
    }

    private boolean isPatrolOrder() {
        return orderMode == OrderMode.PATROL;
    }

    private Player getOrderPlayer() {
        if (!(level() instanceof ServerLevel serverLevel) || orderPlayerUuid == null) {
            return null;
        }
        return serverLevel.getPlayerByUUID(orderPlayerUuid);
    }

    private Component orderModeName() {
        return Component.translatable(orderMode.translationKey);
    }

    // --- Interaction ---

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            ItemStack held = player.getItemInHand(hand);
            if (player.isShiftKeyDown()) {
                setCombatAiEnabled(!isCombatAiEnabled());
                player.sendSystemMessage(Component.translatable(isCombatAiEnabled()
                        ? "message.piranport.ship_girl_combat_on"
                        : "message.piranport.ship_girl_combat_off"));
            } else if (held.is(ModItems.RICHELIEU_COMMAND_SWORD.get())) {
                cycleOrder(player);
            } else if (held.is(ModItems.ABYSSAL_REPORT.get())) {
                handleAbyssalReport(player, held);
            } else if (held.is(ModItems.REPAIR_KIT.get())) {
                handleRepairKit(player, held);
            } else if (held.is(ModItems.PORTAL_ACTIVATION_CORE.get())) {
                handlePortalCore(player, held);
            } else if (held.is(ModItems.EXP_SHELL.get())) {
                handleDrillBranch(player, held);
            } else if (held.is(ModItems.AVIATION_FUEL.get())) {
                handleAirCoverBranch(player, held);
            } else if (isChaosShard(held)) {
                handleChaosShard(player, held);
            } else if (isNationalFlag(held)) {
                handleNationalFlag(player, held);
            } else if (held.is(ModItems.FUEL.get())) {
                acceptSupply(player, held);
            } else {
                sendDialogue(player);
                sendStatus(player);
                sendQuestHint(player);
                setTradingPlayer(player);
                openTradingScreen(player, Component.translatable("entity.piranport.ship_girl"), 1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    private void cycleOrder(Player player) {
        if (orderMode == OrderMode.PATROL) {
            orderMode = OrderMode.FOLLOW;
            orderPlayerUuid = player.getUUID();
            orderHomePos = blockPosition();
        } else if (orderMode == OrderMode.FOLLOW) {
            orderMode = OrderMode.HOLD_POSITION;
            orderPlayerUuid = null;
            orderHomePos = blockPosition();
            getNavigation().stop();
        } else {
            orderMode = OrderMode.PATROL;
            orderPlayerUuid = null;
            orderHomePos = null;
        }

        player.sendSystemMessage(Component.translatable(
                "message.piranport.ship_girl_order_set", orderModeName()));
    }

    private void sendDialogue(Player player) {
        String key;
        if (!combatAiEnabled) {
            key = "message.piranport.ship_girl_dialogue_combat_off";
        } else if (orderMode == OrderMode.FOLLOW) {
            key = "message.piranport.ship_girl_dialogue_follow";
        } else if (orderMode == OrderMode.HOLD_POSITION) {
            key = "message.piranport.ship_girl_dialogue_hold";
        } else {
            String[] narrativeDialogues = narrativeDialogueKeys(player);
            String[] skinDialogues = skinFactionDialogueKeys();
            if (rapport >= 80 && random.nextFloat() < 0.24f) {
                key = pick(HIGH_RAPPORT_DIALOGUE_KEYS);
            } else if (skinDialogues.length > 0 && random.nextFloat() < 0.30f) {
                key = pick(skinDialogues);
            } else if (random.nextFloat() < 0.72f) {
                key = pick(narrativeDialogues);
            } else {
                key = pick(DIALOGUE_KEYS);
            }
        }
        player.sendSystemMessage(Component.translatable(key));
    }

    private String[] narrativeDialogueKeys(Player player) {
        if (questStage != QuestStage.COMPLETE) {
            return switch (questStage) {
                case INTEL -> INTEL_DIALOGUE_KEYS;
                case PREPARATION -> PREPARATION_DIALOGUE_KEYS;
                case CALIBRATION -> CALIBRATION_DIALOGUE_KEYS;
                case COMPLETE -> COMPLETE_DIALOGUE_KEYS;
            };
        }

        if (followUpStage == FollowUpStage.FLAGSHIP_DEBRIEF) {
            if (player instanceof ServerPlayer serverPlayer
                    && ModAdvancements.has(serverPlayer, "story/defeat_abyssal_flagship")) {
                return FOLLOWUP_FLAGSHIP_DIALOGUE_KEYS;
            }
            return COMPLETE_DIALOGUE_KEYS;
        }
        if (followUpStage == FollowUpStage.SHARD_STABILIZATION) {
            return FOLLOWUP_SHARD_DIALOGUE_KEYS;
        }
        if (followUpStage == FollowUpStage.ALLIANCE_CONTACT) {
            return FOLLOWUP_ALLIANCE_DIALOGUE_KEYS;
        }
        if (completedBranchCount() >= BRANCH_FLAGS.length) {
            return BRANCH_COMPLETE_DIALOGUE_KEYS;
        }
        return BRANCH_DIALOGUE_KEYS;
    }

    private String[] skinFactionDialogueKeys() {
        return switch (getSkinVariant()) {
            case 4, 8, 9, 11, 13 -> FACTION_J_DIALOGUE_KEYS;
            case 5, 6, 7, 14, 15 -> FACTION_C_DIALOGUE_KEYS;
            case 12, 17 -> FACTION_G_DIALOGUE_KEYS;
            case 10, 16 -> FACTION_I_DIALOGUE_KEYS;
            case 18, 22 -> FACTION_E_DIALOGUE_KEYS;
            case 19 -> FACTION_F_DIALOGUE_KEYS;
            case 20, 21 -> FACTION_U_DIALOGUE_KEYS;
            default -> new String[0];
        };
    }

    private String pick(String[] keys) {
        return keys[random.nextInt(keys.length)];
    }

    private void sendStatus(Player player) {
        player.sendSystemMessage(Component.translatable(
                "message.piranport.ship_girl_status",
                orderModeName(),
                Component.translatable(isCombatAiEnabled()
                        ? "message.piranport.ship_girl_status_combat_on"
                        : "message.piranport.ship_girl_status_combat_off"),
                Math.round(getHealth()),
                Math.round(getMaxHealth()),
                rapport,
                rapportTierName()));
    }

    private void sendQuestHint(Player player) {
        if (questStage == QuestStage.COMPLETE) {
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.ship_girl_followup_status",
                    Component.translatable(followUpStage.translationKey)));
            if (isBranchReady()) {
                player.sendSystemMessage(Component.translatable(
                        "message.piranport.ship_girl_branch_status",
                        completedBranchCount(),
                        BRANCH_FLAGS.length));
            }
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.ship_girl_quest_status",
                    Component.translatable(questStage.translationKey)));
        }
    }

    private void handleAbyssalReport(Player player, ItemStack held) {
        if (questStage == QuestStage.INTEL) {
            consumeOne(player, held);
            giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 1));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 120, 0));
            questStage = QuestStage.PREPARATION;
            if (player instanceof ServerPlayer serverPlayer) {
                ModAdvancements.award(serverPlayer, "story/turn_in_intel");
                ModAdvancements.award(serverPlayer, "story/ship_girl_story_intel");
            }
            addRapport(player, 10);
            player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_quest_intel_done"));
            playQuestFeedback(player, ParticleTypes.ENCHANT, 24, 1.15f);
            return;
        }
        if (questStage == QuestStage.COMPLETE && followUpStage == FollowUpStage.FLAGSHIP_DEBRIEF) {
            handleFlagshipDebrief(player, held);
            return;
        }
        if (isBranchReady()) {
            handleReconBranch(player, held);
            return;
        }
        completeIntelTurnIn(player, held);
    }

    private void handleRepairKit(Player player, ItemStack held) {
        if (isBranchReady()) {
            handleMaintenanceBranch(player, held);
            return;
        }
        if (questStage != QuestStage.PREPARATION) {
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.ship_girl_quest_not_ready",
                    Component.translatable(questStage.translationKey)));
            playNoSound(player);
            return;
        }

        consumeOne(player, held);
        heal(18.0f);
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 1));
        giveOrDrop(player, new ItemStack(ModItems.AVIATION_FUEL.get(), 3));
        questStage = QuestStage.CALIBRATION;
        addRapport(player, 10);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/ship_girl_story_preparation");
        }
        player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_quest_preparation_done"));
        playQuestFeedback(player, ParticleTypes.HAPPY_VILLAGER, 18, 1.25f);
    }

    private void handlePortalCore(Player player, ItemStack held) {
        if (questStage != QuestStage.CALIBRATION) {
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.ship_girl_quest_not_ready",
                    Component.translatable(questStage.translationKey)));
            playNoSound(player);
            return;
        }

        consumeOne(player, held);
        giveOrDrop(player, new ItemStack(ModItems.SHIP_GIRL_CONTRACT.get(), 1));
        giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 2));
        giveOrDrop(player, new ItemStack(ModItems.FLAG_J.get(), 1));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 90, 0));
        questStage = QuestStage.COMPLETE;
        addRapport(player, 15);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/ship_girl_story_complete");
        }
        player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_quest_complete"));
        playQuestFeedback(player, ParticleTypes.FIREWORK, 36, 1.45f);
    }

    private void handleFlagshipDebrief(Player player, ItemStack held) {
        if (player instanceof ServerPlayer serverPlayer
                && !ModAdvancements.has(serverPlayer, "story/defeat_abyssal_flagship")) {
            player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_followup_need_flagship"));
            playNoSound(player);
            return;
        }

        consumeOne(player, held);
        giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 2));
        giveOrDrop(player, new ItemStack(ModItems.AVIATION_FUEL.get(), 4));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 180, 0));
        followUpStage = FollowUpStage.SHARD_STABILIZATION;
        addRapport(player, 12);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/ship_girl_story_flagship_debrief");
        }
        player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_followup_flagship_done"));
        playQuestFeedback(player, ParticleTypes.SOUL_FIRE_FLAME, 30, 1.2f);
    }

    private void handleChaosShard(Player player, ItemStack held) {
        if (isBranchReady()) {
            handleShardResearchBranch(player, held);
            return;
        }
        if (!isReadyForFollowUp(player, FollowUpStage.SHARD_STABILIZATION)) {
            return;
        }

        consumeOne(player, held);
        giveOrDrop(player, new ItemStack(ModItems.REPAIR_KIT.get(), 1));
        giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 2));
        player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20 * 120, 0));
        followUpStage = FollowUpStage.ALLIANCE_CONTACT;
        addRapport(player, 12);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/ship_girl_story_shard_stabilization");
        }
        player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_followup_shard_done"));
        playQuestFeedback(player, ParticleTypes.REVERSE_PORTAL, 34, 0.9f);
    }

    private void handleNationalFlag(Player player, ItemStack held) {
        if (isBranchReady()) {
            handleAllianceSupplyBranch(player, held);
            return;
        }
        if (!isReadyForFollowUp(player, FollowUpStage.ALLIANCE_CONTACT)) {
            return;
        }

        consumeOne(player, held);
        giveOrDrop(player, new ItemStack(ModItems.SHIP_GIRL_CONTRACT.get(), 1));
        giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 3));
        giveOrDrop(player, new ItemStack(ModItems.AVIATION_FUEL.get(), 6));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 120, 0));
        followUpStage = FollowUpStage.COMPLETE;
        addRapport(player, 15);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/ship_girl_story_alliance");
        }
        player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_followup_alliance_done"));
        playQuestFeedback(player, ParticleTypes.FIREWORK, 42, 1.55f);
    }

    private boolean isReadyForFollowUp(Player player, FollowUpStage expectedStage) {
        if (questStage != QuestStage.COMPLETE) {
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.ship_girl_quest_not_ready",
                    Component.translatable(questStage.translationKey)));
            playNoSound(player);
            return false;
        }
        if (followUpStage != expectedStage) {
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.ship_girl_followup_not_ready",
                    Component.translatable(followUpStage.translationKey)));
            playNoSound(player);
            return false;
        }
        return true;
    }

    private boolean isBranchReady() {
        return questStage == QuestStage.COMPLETE && followUpStage == FollowUpStage.COMPLETE;
    }

    private void handleReconBranch(Player player, ItemStack held) {
        boolean first = markBranchCompleted(BRANCH_RECON);
        consumeOne(player, held);
        giveOrDrop(player, new ItemStack(ModItems.SMALL_FLARE_SHELL.get(), first ? 6 : 2));
        giveOrDrop(player, new ItemStack(ModItems.SMALL_SMOKE_SHELL.get(), first ? 6 : 2));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 180, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 60, 0));
        addRapport(player, first ? 12 : 2);
        awardBranchAdvancement(player, "story/ship_girl_branch_recon", first);
        player.sendSystemMessage(Component.translatable(first
                ? "message.piranport.ship_girl_branch_recon_done"
                : "message.piranport.ship_girl_branch_recon_repeat"));
        playQuestFeedback(player, ParticleTypes.ENCHANT, first ? 28 : 14, 1.25f);
    }

    private void handleMaintenanceBranch(Player player, ItemStack held) {
        boolean first = markBranchCompleted(BRANCH_MAINTENANCE);
        consumeOne(player, held);
        heal(first ? 24.0f : 10.0f);
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 12, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 0));
        giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), first ? 2 : 1));
        addRapport(player, first ? 10 : 2);
        awardBranchAdvancement(player, "story/ship_girl_branch_maintenance", first);
        player.sendSystemMessage(Component.translatable(first
                ? "message.piranport.ship_girl_branch_maintenance_done"
                : "message.piranport.ship_girl_branch_maintenance_repeat"));
        playQuestFeedback(player, ParticleTypes.HEART, first ? 24 : 10, 1.35f);
    }

    private void handleDrillBranch(Player player, ItemStack held) {
        if (!isBranchReady()) {
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.ship_girl_branch_locked",
                    Component.translatable(followUpStage.translationKey)));
            playNoSound(player);
            return;
        }
        boolean first = markBranchCompleted(BRANCH_DRILL);
        consumeOne(player, held);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 90, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 90, 0));
        giveOrDrop(player, new ItemStack(ModItems.RAW_ALUMINUM.get(), first ? 10 : 4));
        addRapport(player, first ? 10 : 2);
        awardBranchAdvancement(player, "story/ship_girl_branch_drill", first);
        player.sendSystemMessage(Component.translatable(first
                ? "message.piranport.ship_girl_branch_drill_done"
                : "message.piranport.ship_girl_branch_drill_repeat"));
        playQuestFeedback(player, ParticleTypes.CRIT, first ? 26 : 12, 1.45f);
    }

    private void handleAirCoverBranch(Player player, ItemStack held) {
        if (!isBranchReady()) {
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.ship_girl_branch_locked",
                    Component.translatable(followUpStage.translationKey)));
            playNoSound(player);
            return;
        }
        boolean first = markBranchCompleted(BRANCH_AIR_COVER);
        consumeOne(player, held);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 120, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 90, 0));
        giveOrDrop(player, new ItemStack(ModItems.SMALL_FLARE_SHELL.get(), first ? 4 : 2));
        if (first) {
            giveOrDrop(player, new ItemStack(ModItems.ABYSSAL_REPORT.get(), 1));
        }
        addRapport(player, first ? 10 : 2);
        awardBranchAdvancement(player, "story/ship_girl_branch_air_cover", first);
        player.sendSystemMessage(Component.translatable(first
                ? "message.piranport.ship_girl_branch_air_cover_done"
                : "message.piranport.ship_girl_branch_air_cover_repeat"));
        playQuestFeedback(player, ParticleTypes.CLOUD, first ? 30 : 12, 1.55f);
    }

    private void handleShardResearchBranch(Player player, ItemStack held) {
        boolean first = markBranchCompleted(BRANCH_SHARD_RESEARCH);
        consumeOne(player, held);
        giveOrDrop(player, new ItemStack(ModItems.RAW_ALUMINUM.get(), first ? 16 : 6));
        giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), first ? 2 : 1));
        player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20 * 150, 0));
        addRapport(player, first ? 12 : 2);
        awardBranchAdvancement(player, "story/ship_girl_branch_shard_research", first);
        player.sendSystemMessage(Component.translatable(first
                ? "message.piranport.ship_girl_branch_shard_done"
                : "message.piranport.ship_girl_branch_shard_repeat"));
        playQuestFeedback(player, ParticleTypes.REVERSE_PORTAL, first ? 32 : 14, 0.95f);
    }

    private void handleAllianceSupplyBranch(Player player, ItemStack held) {
        boolean first = markBranchCompleted(BRANCH_ALLIANCE_SUPPLY);
        consumeOne(player, held);
        giveOrDrop(player, new ItemStack(ModItems.AVIATION_FUEL.get(), first ? 8 : 3));
        giveOrDrop(player, new ItemStack(ModItems.REPAIR_KIT.get(), first ? 2 : 1));
        if (first) {
            giveOrDrop(player, new ItemStack(ModItems.SHIP_GIRL_CONTRACT.get(), 1));
        }
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 150, 0));
        addRapport(player, first ? 12 : 2);
        awardBranchAdvancement(player, "story/ship_girl_branch_alliance_supply", first);
        player.sendSystemMessage(Component.translatable(first
                ? "message.piranport.ship_girl_branch_alliance_done"
                : "message.piranport.ship_girl_branch_alliance_repeat"));
        playQuestFeedback(player, ParticleTypes.FIREWORK, first ? 36 : 16, 1.65f);
    }

    private void awardBranchAdvancement(Player player, String path, boolean firstCompletion) {
        if (firstCompletion && player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, path);
        }
    }

    private boolean markBranchCompleted(int branchFlag) {
        if ((branchMask & branchFlag) != 0) {
            return false;
        }
        branchMask |= branchFlag;
        return true;
    }

    private int completedBranchCount() {
        int count = 0;
        for (int branchFlag : BRANCH_FLAGS) {
            if ((branchMask & branchFlag) != 0) {
                count++;
            }
        }
        return count;
    }

    private void addRapport(Player player, int amount) {
        addRapport(player, amount, true);
    }

    private void addRapport(Player player, int amount, boolean notify) {
        int previous = rapport;
        rapport = Math.max(0, Math.min(MAX_RAPPORT, rapport + amount));
        entityData.set(DATA_RAPPORT, rapport);
        if (rapport <= previous) {
            return;
        }
        offers = null;
        if (notify) {
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.ship_girl_rapport_gain",
                    rapport,
                    rapportTierName()));
        }
        if (rapport >= 80 && player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/ship_girl_high_rapport");
        }
    }

    private Component rapportTierName() {
        String key;
        if (rapport >= 80) {
            key = "message.piranport.ship_girl_rapport_tier_core";
        } else if (rapport >= 50) {
            key = "message.piranport.ship_girl_rapport_tier_comrade";
        } else if (rapport >= 20) {
            key = "message.piranport.ship_girl_rapport_tier_trusted";
        } else {
            key = "message.piranport.ship_girl_rapport_tier_contact";
        }
        return Component.translatable(key);
    }

    private boolean isChaosShard(ItemStack stack) {
        return stack.is(ModItems.CHAOS_SHARD_ALPHA.get())
                || stack.is(ModItems.CHAOS_SHARD_BETA.get())
                || stack.is(ModItems.CHAOS_SHARD_GAMMA.get())
                || stack.is(ModItems.CHAOS_SHARD_DELTA.get())
                || stack.is(ModItems.CHAOS_SHARD_EPSILON.get())
                || stack.is(ModItems.CHAOS_SHARD_ZETA.get())
                || stack.is(ModItems.CHAOS_SHARD_ETA.get())
                || stack.is(ModItems.CHAOS_SHARD_THETA.get())
                || stack.is(ModItems.CHAOS_SHARD_IOTA.get());
    }

    private boolean isNationalFlag(ItemStack stack) {
        return stack.is(ModItems.FLAG_J.get())
                || stack.is(ModItems.FLAG_E.get())
                || stack.is(ModItems.FLAG_U.get())
                || stack.is(ModItems.FLAG_G.get())
                || stack.is(ModItems.FLAG_F.get())
                || stack.is(ModItems.FLAG_I.get())
                || stack.is(ModItems.FLAG_C.get());
    }

    private void completeIntelTurnIn(Player player, ItemStack held) {
        consumeOne(player, held);
        giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 1));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 120, 0));
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/turn_in_intel");
        }
        addRapport(player, 3);
        player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_intel_turn_in"));
    }

    private void acceptSupply(Player player, ItemStack held) {
        if (getHealth() >= getMaxHealth()) {
            player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_supply_full"));
            return;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        heal(12.0f);
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0));
        addRapport(player, 2);
        player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_supply_accept"));
    }

    private void consumeOne(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void playNoSound(Player player) {
        level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.45f, 1.0f);
    }

    private void playQuestFeedback(Player player, net.minecraft.core.particles.ParticleOptions particle,
                                   int count, float pitch) {
        level().playSound(null, blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL,
                0.7f, pitch);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle,
                    getX(), getY() + 1.1, getZ(),
                    count, 0.45, 0.45, 0.45, 0.03);
        }
    }

    // --- Trading ---

    @Override
    public void setTradingPlayer(Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            offers = createOffers();
        }
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        tradeXp += offer.getXp();
        if (tradingPlayer instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/trade_with_ship_girl");
            addRapport(serverPlayer, 1, false);
        }
        playSound(getNotifyTradeSound(), 0.6f, 1.0f + random.nextFloat() * 0.2f);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return tradeXp;
    }

    @Override
    public void overrideXp(int xp) {
        tradeXp = Math.max(0, xp);
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide();
    }

    private MerchantOffers createOffers() {
        MerchantOffers result = new MerchantOffers();
        result.add(new MerchantOffer(
                new ItemCost(ModItems.ABYSSAL_REPORT.get(), 1),
                new ItemStack(ModItems.EXP_SHELL.get(), 1),
                16, 4, 0.05f));
        result.add(new MerchantOffer(
                new ItemCost(ModItems.RAW_ALUMINUM.get(), 8),
                Optional.of(new ItemCost(ModItems.ABYSSAL_REPORT.get(), 1)),
                new ItemStack(ModItems.REPAIR_KIT.get(), 1),
                6, 8, 0.05f));
        result.add(new MerchantOffer(
                new ItemCost(ModItems.FUEL.get(), 3),
                new ItemStack(ModItems.AVIATION_FUEL.get(), 2),
                12, 3, 0.05f));
        result.add(new MerchantOffer(
                new ItemCost(Items.PAPER, 12),
                Optional.of(new ItemCost(ModItems.ABYSSAL_REPORT.get(), 1)),
                new ItemStack(ModItems.FLAG_J.get(), 1),
                4, 6, 0.05f));
        result.add(new MerchantOffer(
                new ItemCost(ModItems.PORTAL_ACTIVATION_CORE.get(), 1),
                Optional.of(new ItemCost(ModItems.ABYSSAL_REPORT.get(), 1)),
                new ItemStack(ModItems.SHIP_GIRL_CONTRACT.get(), 1),
                2, 12, 0.05f));
        if (rapport >= 20) {
            result.add(new MerchantOffer(
                    new ItemCost(ModItems.ABYSSAL_REPORT.get(), 1),
                    Optional.of(new ItemCost(ModItems.RAW_ALUMINUM.get(), 6)),
                    new ItemStack(ModItems.AVIATION_FUEL.get(), 4),
                    8, 6, 0.05f));
        }
        if (rapport >= 50) {
            result.add(new MerchantOffer(
                    new ItemCost(ModItems.EXP_SHELL.get(), 1),
                    Optional.of(new ItemCost(ModItems.RAW_ALUMINUM.get(), 8)),
                    new ItemStack(ModItems.REPAIR_KIT.get(), 2),
                    6, 8, 0.05f));
        }
        if (rapport >= 80) {
            result.add(new MerchantOffer(
                    new ItemCost(ModItems.ABYSSAL_REPORT.get(), 2),
                    Optional.of(new ItemCost(ModItems.FLAG_J.get(), 1)),
                    new ItemStack(ModItems.SHIP_GIRL_CONTRACT.get(), 1),
                    2, 16, 0.05f));
        }
        return result;
    }

    // --- Don't retaliate against players ---

    // NOTE: 故意不加 HurtByTargetGoal —— ShipGirl 不应还手打玩家
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) {
            setTarget(null);
        }
        return super.hurt(source, amount);
    }

    // --- Rendering ---

    @Override
    public boolean isCurrentlyGlowing() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return false;
    }

    // --- Persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("CombatAi", combatAiEnabled);
        tag.putString("OrderMode", orderMode.name());
        tag.putString("QuestStage", questStage.name());
        tag.putString("FollowUpStage", followUpStage.name());
        tag.putInt("SkinVariant", getSkinVariant());
        tag.putInt("TradeXp", tradeXp);
        tag.putInt("Rapport", rapport);
        tag.putInt("BranchMask", branchMask);
        if (orderPlayerUuid != null) {
            tag.putUUID("OrderPlayer", orderPlayerUuid);
        }
        if (orderHomePos != null) {
            tag.putInt("OrderHomeX", orderHomePos.getX());
            tag.putInt("OrderHomeY", orderHomePos.getY());
            tag.putInt("OrderHomeZ", orderHomePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CombatAi")) {
            combatAiEnabled = tag.getBoolean("CombatAi");
        }
        if (tag.contains("OrderMode")) {
            try {
                orderMode = OrderMode.valueOf(tag.getString("OrderMode"));
            } catch (IllegalArgumentException ignored) {
                orderMode = OrderMode.PATROL;
            }
        }
        if (tag.contains("QuestStage")) {
            try {
                questStage = QuestStage.valueOf(tag.getString("QuestStage"));
            } catch (IllegalArgumentException ignored) {
                questStage = QuestStage.INTEL;
            }
        }
        if (tag.contains("FollowUpStage")) {
            try {
                followUpStage = FollowUpStage.valueOf(tag.getString("FollowUpStage"));
            } catch (IllegalArgumentException ignored) {
                followUpStage = FollowUpStage.FLAGSHIP_DEBRIEF;
            }
        }
        if (tag.contains("SkinVariant")) {
            setSkinVariant(tag.getInt("SkinVariant"));
        }
        tradeXp = tag.getInt("TradeXp");
        rapport = Math.max(0, Math.min(MAX_RAPPORT, tag.getInt("Rapport")));
        entityData.set(DATA_RAPPORT, rapport);
        branchMask = tag.getInt("BranchMask");
        orderPlayerUuid = tag.hasUUID("OrderPlayer") ? tag.getUUID("OrderPlayer") : null;
        if (tag.contains("OrderHomeX") && tag.contains("OrderHomeY") && tag.contains("OrderHomeZ")) {
            orderHomePos = new BlockPos(
                    tag.getInt("OrderHomeX"),
                    tag.getInt("OrderHomeY"),
                    tag.getInt("OrderHomeZ"));
        } else {
            orderHomePos = null;
        }
    }

    private static class PatrolStrollGoal extends RandomStrollGoal {
        private final ShipGirlEntity shipGirl;

        PatrolStrollGoal(ShipGirlEntity shipGirl, double speedModifier) {
            super(shipGirl, speedModifier);
            this.shipGirl = shipGirl;
        }

        @Override
        public boolean canUse() {
            return shipGirl.isPatrolOrder() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return shipGirl.isPatrolOrder() && super.canContinueToUse();
        }
    }

    private static class FollowOrderGoal extends Goal {
        private final ShipGirlEntity shipGirl;
        private final double speedModifier;
        private final float stopDistance;
        private final float startDistance;
        private Player owner;

        FollowOrderGoal(ShipGirlEntity shipGirl, double speedModifier,
                        float stopDistance, float startDistance) {
            this.shipGirl = shipGirl;
            this.speedModifier = speedModifier;
            this.stopDistance = stopDistance;
            this.startDistance = startDistance;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!shipGirl.isFollowingOrder() || shipGirl.getTarget() != null) return false;
            Player player = shipGirl.getOrderPlayer();
            if (player == null || player.isSpectator() || !player.isAlive()) return false;
            if (shipGirl.distanceToSqr(player) < startDistance * startDistance) return false;
            owner = player;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null
                    && shipGirl.isFollowingOrder()
                    && shipGirl.getTarget() == null
                    && owner.isAlive()
                    && !owner.isSpectator()
                    && shipGirl.distanceToSqr(owner) > stopDistance * stopDistance;
        }

        @Override
        public void stop() {
            owner = null;
            shipGirl.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (owner == null) return;
            shipGirl.getLookControl().setLookAt(owner, 30.0f, 30.0f);
            shipGirl.getNavigation().moveTo(owner, speedModifier);
        }
    }

    private static class HoldPositionGoal extends Goal {
        private final ShipGirlEntity shipGirl;
        private final double speedModifier;

        HoldPositionGoal(ShipGirlEntity shipGirl, double speedModifier) {
            this.shipGirl = shipGirl;
            this.speedModifier = speedModifier;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return shipGirl.isHoldingPositionOrder() && shipGirl.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            BlockPos home = shipGirl.orderHomePos;
            if (home == null || shipGirl.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) <= 4.0) {
                shipGirl.getNavigation().stop();
                return;
            }
            shipGirl.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, speedModifier);
        }
    }
}
