package com.piranport.combat.data;

import com.piranport.component.LoadedAmmo;
import com.piranport.component.WeaponCooldown;
import com.piranport.component.SlotCooldowns;
import com.piranport.combat.TransformationManager;
import com.piranport.registry.ModDataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** 清理离开快捷栏的未完成装填计时，保留已经装入武器的数据组件。 */
@net.neoforged.fml.common.EventBusSubscriber(modid = com.piranport.PiranPort.MOD_ID)
public final class WeaponReloadLifecycle {
    private static final java.util.Map<java.util.UUID, java.util.IdentityHashMap<ItemStack, Integer>> HOTBAR_SLOTS
            = new java.util.HashMap<>();

    private WeaponReloadLifecycle() {}

    public static void tick(ItemStack stack, Level level, Entity holder, int slot) {
        if (level.isClientSide() || !(holder instanceof Player player)) return;
        java.util.IdentityHashMap<ItemStack, Integer> slots = HOTBAR_SLOTS.computeIfAbsent(
                player.getUUID(), ignored -> new java.util.IdentityHashMap<>());
        if (slot >= 0 && slot < 9 && player.getInventory().getItem(slot) == stack) {
            slots.put(stack, slot);
        }
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        finishPendingTorpedoReloads(player);
        java.util.IdentityHashMap<ItemStack, Integer> slots = HOTBAR_SLOTS.get(player.getUUID());
        if (slots == null || slots.isEmpty()) return;
        Inventory inventory = player.getInventory();
        var iterator = slots.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ItemStack stack = entry.getKey();
            int oldSlot = entry.getValue();
            int currentSlot = findHotbarSlot(inventory, stack);
            if (currentSlot >= 0) {
                entry.setValue(currentSlot);
                // 武器在快捷栏内换位时，把核心上的槽位冷却一并迁移到新槽位；
                // 否则旧槽位会留一条指向不存在武器的冷却，既挡不了旧槽、也让新槽漏过冷却检查。
                long gameTime = player.level().getGameTime();
                ItemStack core = TransformationManager.findTransformedCore(player);
                if (!core.isEmpty()) {
                    SlotCooldowns cooldowns = core.getOrDefault(
                            ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
                    if (cooldowns.isOnCooldown(oldSlot, gameTime)) {
                        long remaining = cooldowns.endTick().get(oldSlot) - gameTime;
                        // 用无属主重载保持剩余 tick 原样迁移，避免属主版再叠加一次测试冷却覆盖。
                        core.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                                cooldowns.withoutSlotCooldown(oldSlot)
                                        .withSlotCooldown(currentSlot, (int) remaining, gameTime));
                    }
                }
                continue;
            }
            // 海上鱼雷装填在读条期间不扣物品、也不可取消：武器离开快捷栏时保留冷却和待结算状态，
            // 由 finishPendingTorpedoReloads 到期统一扣除（2026-10-19 修订）。
            if (stack.get(ModDataComponents.PENDING_TORPEDO_RELOAD.get()) != null) {
                iterator.remove();
                continue;
            }
            WeaponCooldown cooldown = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
            boolean reloadWasActive = cooldown != null && cooldown.isOnCooldown(player.level().getGameTime());
            LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (reloadWasActive && loaded.hasAmmo()) {
                ResourceLocation ammoId = ResourceLocation.tryParse(loaded.ammoItemId());
                if (ammoId != null) {
                    var ammo = BuiltInRegistries.ITEM.get(ammoId);
                    if (ammo != net.minecraft.world.item.Items.AIR) {
                        ItemStack refund = new ItemStack(ammo, loaded.count());
                        if (!inventory.add(refund)) player.drop(refund, false);
                    }
                }
                stack.remove(ModDataComponents.LOADED_AMMO.get());
            }
            stack.remove(ModDataComponents.WEAPON_COOLDOWN.get());
            ItemStack core = TransformationManager.findTransformedCore(player);
            if (!core.isEmpty()) {
                SlotCooldowns cooldowns = core.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
                SlotCooldowns cleared = cooldowns.withoutSlotCooldown(oldSlot);
                if (cleared != cooldowns) core.set(ModDataComponents.SLOT_COOLDOWNS.get(), cleared);
            }
            iterator.remove();
        }
        if (slots.isEmpty()) HOTBAR_SLOTS.remove(player.getUUID());
    }

    /**
     * 结算到期（WEAPON_COOLDOWN 结束）的海上鱼雷装填：
     * 此时才从背包扣除同口径鱼雷，并一次性装满膛内。读条期间物品不足不扣、不取消。
     */
    private static void finishPendingTorpedoReloads(Player player) {
        Inventory inv = player.getInventory();
        long now = player.level().getGameTime();
        for (int i = 0; i < inv.items.size(); i++) {
            finishOneTorpedoReload(player, inv, inv.items.get(i), now);
        }
        finishOneTorpedoReload(player, inv, inv.offhand.get(0), now);
    }

    private static void finishOneTorpedoReload(Player player, Inventory inv, ItemStack stack, long now) {
        if (stack.isEmpty() || !(stack.getItem() instanceof com.piranport.item.TorpedoLauncherItem launcher)) return;
        com.piranport.component.PendingTorpedoReload pending =
                stack.get(ModDataComponents.PENDING_TORPEDO_RELOAD.get());
        if (pending == null || !pending.isValid()) return;
        WeaponCooldown cooldown = stack.getOrDefault(ModDataComponents.WEAPON_COOLDOWN.get(), WeaponCooldown.EMPTY);
        if (cooldown.isOnCooldown(now)) return;

        ResourceLocation ammoId = ResourceLocation.tryParse(pending.ammoItemId());
        net.minecraft.world.item.Item ammo = ammoId == null
                ? net.minecraft.world.item.Items.AIR
                : BuiltInRegistries.ITEM.get(ammoId);
        if (ammo == net.minecraft.world.item.Items.AIR) {
            stack.remove(ModDataComponents.PENDING_TORPEDO_RELOAD.get());
            return;
        }

        // 创造弹药库勾选了该弹种时，读条到期不扣任何实体鱼雷。
        // WHY 必须在这里再判一次：R 键开始装填时只记 pending，真正扣弹在到期结算；
        // 若只在开始处放行，结算仍会按实体数量报「同类弹药不足」并把 pending 清掉。
        boolean infinite = com.piranport.ammo.CreativeAmmoCache.isInfinite(player, ammo);

        int available = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.is(ammo)) available += s.getCount();
        }
        ItemStack offhand = inv.offhand.get(0);
        if (!offhand.isEmpty() && offhand.is(ammo)) available += offhand.getCount();

        if (!infinite && available < pending.count()) {
            stack.remove(ModDataComponents.PENDING_TORPEDO_RELOAD.get());
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.piranport.insufficient_same_ammo"), true);
            return;
        }

        if (!infinite) {
            int left = pending.count();
            for (int i = 0; i < inv.items.size() && left > 0; i++) {
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.is(ammo)) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                    if (s.getCount() <= 0) inv.setItem(i, ItemStack.EMPTY);
                }
            }
            if (left > 0 && !offhand.isEmpty() && offhand.is(ammo)) {
                offhand.shrink(left);
                if (offhand.getCount() <= 0) inv.offhand.set(0, ItemStack.EMPTY);
            }
        }

        stack.set(ModDataComponents.LOADED_AMMO.get(),
                new LoadedAmmo(launcher.getTubeCount(), pending.ammoItemId()));
        stack.remove(ModDataComponents.PENDING_TORPEDO_RELOAD.get());
    }

    private static int findHotbarSlot(Inventory inventory, ItemStack stack) {
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot) == stack) return slot;
        }
        return -1;
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        HOTBAR_SLOTS.remove(event.getEntity().getUUID());
    }
}
