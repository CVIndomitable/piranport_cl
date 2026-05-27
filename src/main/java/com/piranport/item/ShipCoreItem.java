package com.piranport.item;

import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FlightGroupData;
import com.piranport.component.FuelData;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SelectedAmmoType;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.SanshikiPelletEntity;
import com.piranport.entity.DepthChargeEntity;
import com.piranport.entity.MissileEntity;
import com.piranport.entity.TorpedoEntity;

import com.piranport.network.ShakeEffectPayload;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import com.piranport.registry.ModSounds;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import com.piranport.client.BallisticSolver;
import com.piranport.PiranPort;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Equipable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 舰装核心 — 玩家变身为舰娘的核心物品。
 *
 * <p><b>架构导航</b>（按源文件顺序）：
 * <ol>
 *   <li><b>装备/附魔/燃料条</b> (getEquipmentSlot ~ getBarColor) — Equipable 接口、附魔、燃料耐久条</li>
 *   <li><b>物品交互</b> (overrideOtherStackedOnMe) — 燃料添加、装甲板/装备存储</li>
 *   <li><b>Tooltip</b> (appendHoverText) — 物品提示</li>
 *   <li><b>右键使用</b> (use) — 变身/取消变身、GUI模式切换、无GUI开火</li>
 *   <li><b>武器发射</b> — 已提取到 {@link ShipCoreCombat}，本类仅保留 use() 入口</li>
 *   <li><b>飞机系统</b> — 已提取到 {@link ShipCoreCombat}，本类仅保留 use() 入口</li>
 *   <li><b>武器属性辅助</b> — 已提取到 {@link ShipCoreCombat}</li>
 * </ol>
 *
 * <p><b>线程模型</b>: 服务端主线程。所有方法通过 {@link net.neoforged.neoforge.event.tick.PlayerTickEvent}
 * 驱动，无并发问题。
 */
public class ShipCoreItem extends Item implements Equipable {

    // Phase 12: 口径弹药标签 — 数据包可通过添加物品到这些标签来扩展兼容弹药
    static final TagKey<Item> SMALL_SHELLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "small_shells"));
    static final TagKey<Item> MEDIUM_SHELLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "medium_shells"));
    static final TagKey<Item> LARGE_SHELLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "large_shells"));

    private final ShipType shipType;

    public ShipCoreItem(Properties properties, ShipType shipType) {
        super(properties);
        this.shipType = shipType;
    }

    public ShipType getShipType() {
        return shipType;
    }

    // ===== Equipable 接口实现（支持头盔槽位） =====

    @Override
    public EquipmentSlot getEquipmentSlot() {
        String slotMode = ModCommonConfig.SHIP_CORE_SLOT_MODE.get();
        if ("helmet".equalsIgnoreCase(slotMode) || "chest".equalsIgnoreCase(slotMode)) {
            return EquipmentSlot.HEAD;
        }
        return EquipmentSlot.OFFHAND;
    }

    @Override
    public net.minecraft.core.Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    // ===== 附魔支持 =====

    @Override
    public int getEnchantmentValue() {
        // 返回附魔能力值，类似于铁盔甲（9）或钻石盔甲（10）
        // 根据舰型返回不同的附魔能力值
        return switch (shipType) {
            case SMALL -> 8;      // 驱逐舰：类似锁链甲
            case MEDIUM -> 9;     // 巡洋舰：类似铁甲
            case LARGE -> 10;     // 战列舰：类似钻石甲
            case SUBMARINE -> 7;  // 潜艇：较低附魔能力
        };
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        // 允许在附魔台附魔
        return true;
    }

    // ===== Fuel bar (durability-style) =====

    @Override
    public boolean isBarVisible(ItemStack stack) {
        FuelData fuel = stack.get(ModDataComponents.SHIP_CORE_FUEL.get());
        return fuel != null && fuel.currentFuel() < fuel.maxFuel();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FuelData fuel = stack.get(ModDataComponents.SHIP_CORE_FUEL.get());
        if (fuel == null || fuel.maxFuel() == 0) return 0;
        return Math.round(13.0f * fuel.currentFuel() / fuel.maxFuel());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FuelData fuel = stack.get(ModDataComponents.SHIP_CORE_FUEL.get());
        if (fuel == null) return 0xFF00FF00;
        float fraction = fuel.getFraction();
        return net.minecraft.util.Mth.hsvToRgb(fraction / 3.0f, 1.0f, 1.0f);
    }

    /**
     * 验证并修正燃料数据完整性。直接修改 ItemStack，无返回值。
     */
    private void validateAndFixFuelData(ItemStack stack) {
        FuelData fuel = stack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                new FuelData(0, shipType.fuelCapacity));
        if (fuel.maxFuel() < 0 || fuel.currentFuel() < 0) {
            fuel = new FuelData(0, shipType.fuelCapacity);
            stack.set(ModDataComponents.SHIP_CORE_FUEL.get(), fuel);
        }
    }

    /**
     * Bundle-like armor storage for no-GUI mode.
     * - Right-click an ArmorPlateItem onto the core in inventory → stores the plate inside the core.
     * - Right-click the core with an empty cursor → extracts the last stored plate back to cursor.
     * Capacity = shipType.enhancementSlots (2–4 plates).
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other,
            Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;

        // Fuel refueling: lava bucket → +1b fuel (works in both GUI and no-GUI modes)
        if (!other.isEmpty() && other.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
            validateAndFixFuelData(stack);
            FuelData fuel = stack.get(ModDataComponents.SHIP_CORE_FUEL.get());
            if (!fuel.isFull()) {
                stack.set(ModDataComponents.SHIP_CORE_FUEL.get(),
                        fuel.withCurrentFuel(fuel.currentFuel() + 1));
                access.set(new ItemStack(net.minecraft.world.item.Items.BUCKET));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 0.5f, 1.0f);
                return true;
            }
            // Full — feedback but don't consume interaction (allow armor install etc.)
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
            return false;
        }

        // 通用燃料处理：烈焰粉、煤炭块 + 所有熔炉燃料（works in both GUI and no-GUI modes）
        if (!other.isEmpty()) {
            int fuelValue = getFuelValue(other);
            if (fuelValue > 0) {
                validateAndFixFuelData(stack);
                FuelData fuel = stack.get(ModDataComponents.SHIP_CORE_FUEL.get());
                if (!fuel.isFull()) {
                    int totalAdded = 0;
                    while (!other.isEmpty()) {
                        int space = fuel.maxFuel() - fuel.currentFuel() - totalAdded;
                        if (space <= 0) break;
                        int gain = Math.min(fuelValue, space);
                        totalAdded += gain;
                        other.shrink(1);
                    }
                    stack.set(ModDataComponents.SHIP_CORE_FUEL.get(),
                            fuel.withCurrentFuel(fuel.currentFuel() + totalAdded));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 0.5f, 1.0f);
                    return true;
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
                return false;
            }
        }

        // Fuel refueling: fuel item → batch fill (works in both GUI and no-GUI modes)
        if (!other.isEmpty() && other.is(ModItems.FUEL.get())) {
            validateAndFixFuelData(stack);
            FuelData fuel = stack.get(ModDataComponents.SHIP_CORE_FUEL.get());
            if (!fuel.isFull()) {
                int space = fuel.maxFuel() - fuel.currentFuel();
                if (space <= 0) return false;
                int toAdd = Math.min(space, other.getCount());
                stack.set(ModDataComponents.SHIP_CORE_FUEL.get(),
                        fuel.withCurrentFuel(fuel.currentFuel() + toAdd));
                other.shrink(toAdd);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 0.5f, 1.0f);
                return true;
            }
            // Full — feedback but don't consume interaction
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
            return false;
        }

        int capacity = shipType.enhancementSlots;
        ItemContainerContents existing = stack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(capacity, ItemStack.EMPTY);
        existing.copyInto(stored);

        if (!other.isEmpty()) {
            // Insert: cursor has ArmorPlateItem/SonarItem/EngineItem/TorpedoReloadItem → store in first empty slot
            if (!(other.getItem() instanceof ArmorPlateItem) && !(other.getItem() instanceof SonarItem) && !(other.getItem() instanceof EngineItem) && !(other.getItem() instanceof TorpedoReloadItem)) return false;
            for (int i = 0; i < capacity; i++) {
                if (stored.get(i).isEmpty()) {
                    stored.set(i, other.copyWithCount(1));
                    other.shrink(1);
                    stack.set(ModDataComponents.SHIP_CORE_ARMOR.get(),
                            ItemContainerContents.fromItems(stored));
                    return true;
                }
            }
            // Full — play error sound as feedback
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
            return true;
        } else {
            // Extract: cursor is empty → pop last stored plate back to cursor
            for (int i = capacity - 1; i >= 0; i--) {
                if (!stored.get(i).isEmpty()) {
                    access.set(stored.get(i).copy());
                    stored.set(i, ItemStack.EMPTY);
                    stack.set(ModDataComponents.SHIP_CORE_ARMOR.get(),
                            ItemContainerContents.fromItems(stored));
                    return true;
                }
            }
            return false; // nothing stored
        }
    }

    // 辅助方法：获取物品的燃料值（基于熔炉燃烧时间）
    // 烈焰粉特殊处理（不是熔炉燃料但作为燃料源）
    private int getFuelValue(ItemStack stack) {
        if (stack.is(net.minecraft.world.item.Items.BLAZE_POWDER)) return 1;
        int burnTime = stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
        if (burnTime > 0) {
            return Math.max(1, (burnTime + 1599) / 1600); // 向上取整
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // Compute current load + engine bonus so the shift-details block can show
        // real-time current speed alongside empty/full-load speeds.
        int currentTotalLoad = 0;
        double currentEngineSpeedBonus = 0;
        boolean hasCurrentLoad = false;
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            net.minecraft.world.entity.player.Player cp =
                    net.minecraft.client.Minecraft.getInstance().player;
            if (cp != null) {
                currentTotalLoad = com.piranport.combat.TransformationManager
                        .getInventoryWeaponLoad(cp.getInventory())
                        + com.piranport.combat.TransformationManager.getCoreArmorLoad(stack);
                currentEngineSpeedBonus = com.piranport.combat.TransformationManager
                        .getCoreEngineSpeedBonus(stack);
                hasCurrentLoad = true;
            }
        }

        // No-GUI mode: check core from configured slot (offhand or helmet)
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                net.minecraft.world.entity.player.Player clientPlayer =
                        net.minecraft.client.Minecraft.getInstance().player;
                if (clientPlayer != null) {
                    Inventory inv = clientPlayer.getInventory();
                    ItemStack activeCore = com.piranport.combat.TransformationManager.getCoreFromConfiguredSlot(clientPlayer);
                    boolean isActive = activeCore.getItem() instanceof ShipCoreItem
                            && activeCore.getItem() == stack.getItem()
                            && ItemStack.isSameItemSameComponents(activeCore, stack);
                    if (!isActive) {
                        tooltipComponents.add(Component.translatable("tooltip.piranport.core_inactive")
                                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                    }
                    int weaponLoad = com.piranport.combat.TransformationManager
                            .getInventoryWeaponLoad(inv);
                    int armorLoad = com.piranport.combat.TransformationManager
                            .getCoreArmorLoad(stack);
                    tooltipComponents.add(Component.translatable(
                            "container.piranport.load", weaponLoad + armorLoad, shipType.maxLoad));
                    // Show stored armor plates
                    int capacity = shipType.enhancementSlots;
                    ItemContainerContents armorContents = stack.getOrDefault(
                            ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
                    NonNullList<ItemStack> storedArmor = NonNullList.withSize(capacity, ItemStack.EMPTY);
                    armorContents.copyInto(storedArmor);
                    int armorBonus = com.piranport.combat.TransformationManager.getCoreArmorBonus(stack);
                    int totalArmor = shipType.baseArmor + armorBonus;
                    if (armorBonus > 0) {
                        tooltipComponents.add(Component.translatable(
                                "tooltip.piranport.core_armor_with_bonus", totalArmor, armorBonus, capacity));
                    } else {
                        tooltipComponents.add(Component.translatable(
                                "tooltip.piranport.core_armor_slots", totalArmor, capacity));
                    }
                    for (ItemStack s : storedArmor) {
                        if (!s.isEmpty()) {
                            tooltipComponents.add(Component.literal("  • ").append(s.getHoverName()));
                        }
                    }
                }
            }
        // Fuel tank info (both modes)
        FuelData fuel = stack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                new FuelData(0, shipType.fuelCapacity));
        tooltipComponents.add(Component.translatable(
                "tooltip.piranport.fuel_tank", fuel.currentFuel(), fuel.maxFuel()));
        // Shift: core stats
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                if (shipType.healthBonus != 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.health_bonus",
                            (shipType.healthBonus > 0 ? "+" : "") + shipType.healthBonus)
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
                tooltipComponents.add(Component.translatable("tooltip.piranport.core.max_load", shipType.maxLoad)
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
                int armorBonus = com.piranport.combat.TransformationManager.getCoreArmorBonus(stack);
                int totalArmor = shipType.baseArmor + armorBonus;
                if (armorBonus > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.armor_with_bonus",
                            totalArmor, armorBonus)
                            .withStyle(net.minecraft.ChatFormatting.BLUE));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.armor", totalArmor)
                            .withStyle(net.minecraft.ChatFormatting.BLUE));
                }
                if (shipType.armorToughness > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.toughness", shipType.armorToughness)
                            .withStyle(net.minecraft.ChatFormatting.AQUA));
                }
                tooltipComponents.add(Component.translatable("tooltip.piranport.core.empty_speed",
                        String.format("%.2f", shipType.emptySpeed)).withStyle(net.minecraft.ChatFormatting.GREEN));
                tooltipComponents.add(Component.translatable("tooltip.piranport.core.full_speed",
                        String.format("%.2f", shipType.fullLoadSpeed)).withStyle(net.minecraft.ChatFormatting.YELLOW));
                if (currentEngineSpeedBonus > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.engine.speed_bonus",
                            String.format("%.0f", currentEngineSpeedBonus * 100)).withStyle(net.minecraft.ChatFormatting.GREEN));
                }
                if (hasCurrentLoad) {
                    double loadRatio = shipType.maxLoad > 0
                            ? (double) currentTotalLoad / shipType.maxLoad : 0;
                    double currentSpeed = shipType.emptySpeed
                            - (shipType.emptySpeed - shipType.fullLoadSpeed)
                                    * Math.min(loadRatio, 1.0)
                            + currentEngineSpeedBonus;
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.current_speed",
                                    String.format("%.2f", currentSpeed))
                            .withStyle(net.minecraft.ChatFormatting.AQUA));
                }
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }

            // 显示槽位提示
            String slotMode = ModCommonConfig.SHIP_CORE_SLOT_MODE.get();
            String hintKey = "tooltip.piranport.ship_core.slot_hint." +
                ("chest".equalsIgnoreCase(slotMode) ? "chest" :
                 "helmet".equalsIgnoreCase(slotMode) ? "helmet" : "offhand");
            tooltipComponents.add(Component.translatable(hintKey)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    // ===== 右键使用：变身/取消变身/GUI/无GUI开火 =====

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean isTransformed = TransformationManager.isTransformed(stack);

        if (player.isShiftKeyDown()) {
            // No-GUI mode: transformation is automatic (offhand-driven), manual toggle disabled
            return InteractionResultHolder.pass(stack);
        }

        // Right-click (no shift) → recall all airborne aircraft
        // Only when core is in main hand — in no-GUI mode the core sits in offhand and
        // the offhand use() fires after every weapon use(), which would immediately recall
        // any aircraft that was just launched by the main-hand weapon item.
        if (hand == InteractionHand.MAIN_HAND
                && !level.isClientSide && level instanceof ServerLevel sl) {
            int recalled = ShipCoreCombat.recallAllAircraft(sl, player);
            if (recalled > 0) {
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_recalled", recalled), true);
                return InteractionResultHolder.consume(stack);
            }
        }

        if (isTransformed) {
            // No-GUI mode: weapons fire via their own use(); return pass so offhand may trigger
            return InteractionResultHolder.pass(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // ====================================================================
    // 武器发射系统 — 火炮/鱼雷/深弹/导弹
    // 共享 SMALL_SHELLS / MEDIUM_SHELLS / LARGE_SHELLS 标签
    // TODO: 提取到 ShipCoreCombat.java
    // ====================================================================

    // ===== Inventory-mode firing =====

    /**
     * Entry point for weapon items' use() in no-GUI mode.
     * Scans inventory for an active (transformed) ship core, then fires the weapon at the given hand slot.
     * Returns true if firing was attempted (so the weapon can return CONSUME).
     */
    /**
     * 返回 true 表示本次"动作被消费"（走完了 fire 派发流程；各派发分支仍可能因弹药/装填原因失败并
     * 自行提示玩家）。返回 false 表示动作未消费（未找到变身核心 / 被冷却静默阻断），以便调用方返回
     * {@link InteractionResultHolder#pass} 而非 {@code consume}。
     */

    // ====================================================================
    // 以下静态方法已提取到 ShipCoreCombat.java
    // 如需修改这些方法，请编辑 ShipCoreCombat.java
    // ====================================================================

}
