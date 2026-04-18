package com.piranport.dungeon.entity;

import com.piranport.dungeon.DungeonConstants;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A floating loot container spawned when a dungeon enemy is defeated.
 * Players right-click to open and loot. Auto-despawns after timeout.
 */
public class LootShipEntity extends Entity {
    private static final EntityDataAccessor<Integer> SHIP_TIER =
            SynchedEntityData.defineId(LootShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DROPPING =
            SynchedEntityData.defineId(LootShipEntity.class, EntityDataSerializers.BOOLEAN);

    private final SimpleContainer inventory = new SimpleContainer(27);
    private boolean lootGenerated = false;
    private boolean filled = false;
    private int despawnTimer = 0;
    /** Set of player UUIDs who have opened this crate (for scripted tracking). */
    private final java.util.Set<java.util.UUID> openedBy = new java.util.HashSet<>();
    /** Callback fired when the crate lands on water (dropping → stationary). */
    private Runnable onLanded;

    public LootShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static LootShipEntity create(ServerLevel level, double x, double y, double z, int tier) {
        LootShipEntity ship = new LootShipEntity(ModEntityTypes.LOOT_SHIP.get(), level);
        ship.setPos(x, y, z);
        ship.entityData.set(SHIP_TIER, tier);
        return ship;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHIP_TIER, 0);
        builder.define(DROPPING, false);
    }

    @Override
    public void tick() {
        super.tick();

        // Dropping mode: fall until reaching sea level
        if (entityData.get(DROPPING)) {
            if (!level().isClientSide()) {
                setDeltaMovement(0, -0.5, 0);
                move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
                if (getY() <= DungeonConstants.SEA_LEVEL + 0.5) {
                    entityData.set(DROPPING, false);
                    setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    setPos(getX(), DungeonConstants.SEA_LEVEL + 0.5, getZ());
                    if (onLanded != null) {
                        onLanded.run();
                        onLanded = null;
                    }
                }
            } else {
                move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            }
            return;
        }

        if (level().isClientSide()) {
            // Client: gentle bobbing
            if (tickCount % 20 == 0) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SPLASH,
                        getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            }
            return;
        }

        // Server: despawn timer
        despawnTimer++;
        if (despawnTimer > DungeonConstants.LOOT_SHIP_DESPAWN_TICKS) {
            discard();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        if (player instanceof ServerPlayer serverPlayer) {
            if (!lootGenerated) {
                generateLoot(serverPlayer);
                lootGenerated = true;
            }
            openedBy.add(serverPlayer.getUUID());
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> ChestMenu.threeRows(id, inv, inventory),
                    Component.translatable("entity.piranport.loot_ship")
            ));
        }
        return InteractionResult.CONSUME;
    }

    private void generateLoot(ServerPlayer player) {
        int tier = entityData.get(SHIP_TIER);
        String lootPath = switch (tier) {
            case 2 -> "dungeon/elite_ship";
            case 3 -> "dungeon/boss_ship";
            default -> "dungeon/normal_ship";
        };

        ResourceLocation lootId = ResourceLocation.fromNamespaceAndPath("piranport", lootPath);
        ServerLevel serverLevel = (ServerLevel) level();
        ResourceKey<LootTable> lootKey = ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE, lootId);
        LootTable lootTable = serverLevel.getServer().reloadableRegistries()
                .getLootTable(lootKey);

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, position())
                .create(LootContextParamSets.GIFT);

        List<ItemStack> loot = lootTable.getRandomItems(params);
        for (int i = 0; i < loot.size() && i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, loot.get(i));
        }
    }

    public int getShipTier() {
        return entityData.get(SHIP_TIER);
    }

    /** Start in dropping (airdrop) mode — falls to sea level. */
    public void setDropping(boolean dropping) {
        entityData.set(DROPPING, dropping);
    }

    public boolean isDropping() {
        return entityData.get(DROPPING);
    }

    /** Set callback for when the crate finishes falling. */
    public void setOnLanded(Runnable callback) {
        this.onLanded = callback;
    }

    /** Pre-fill inventory with given stacks (bypasses loot table). Idempotent — only
     *  fills empty slots so a re-trigger after reload cannot overwrite items the player
     *  has already moved into the crate. */
    public void fillInventory(List<ItemStack> items) {
        if (filled) return;
        for (int i = 0; i < items.size() && i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, items.get(i));
            }
        }
        filled = true;
        lootGenerated = true; // prevent loot table from overwriting
    }

    /** Returns the set of player UUIDs who have interacted with this crate. */
    public java.util.Set<java.util.UUID> getOpenedBy() {
        return java.util.Set.copyOf(openedBy);
    }

    @Override
    public boolean isPickable() { return true; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ShipTier", entityData.get(SHIP_TIER));
        tag.putBoolean("LootGenerated", lootGenerated);
        tag.putBoolean("Filled", filled);
        tag.putInt("DespawnTimer", despawnTimer);
        tag.putBoolean("Dropping", entityData.get(DROPPING));
        // Persist openedBy UUIDs
        if (!openedBy.isEmpty()) {
            net.minecraft.nbt.ListTag openedList = new net.minecraft.nbt.ListTag();
            for (java.util.UUID uuid : openedBy) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("UUID", uuid);
                openedList.add(entry);
            }
            tag.put("OpenedBy", openedList);
        }
        // Save inventory using vanilla ContainerHelper for components / mod-item compat.
        CompoundTag invTag = new CompoundTag();
        net.minecraft.core.NonNullList<ItemStack> nlist = net.minecraft.core.NonNullList.withSize(
                inventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            nlist.set(i, inventory.getItem(i));
        }
        net.minecraft.world.ContainerHelper.saveAllItems(invTag, nlist, level().registryAccess());
        tag.put("Inventory", invTag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(SHIP_TIER, tag.getInt("ShipTier"));
        lootGenerated = tag.getBoolean("LootGenerated");
        filled = tag.getBoolean("Filled");
        despawnTimer = tag.getInt("DespawnTimer");
        if (tag.getBoolean("Dropping")) {
            entityData.set(DROPPING, true);
        }
        // Restore openedBy UUIDs
        if (tag.contains("OpenedBy")) {
            net.minecraft.nbt.ListTag openedList = tag.getList("OpenedBy", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < openedList.size(); i++) {
                CompoundTag entry = openedList.getCompound(i);
                if (entry.hasUUID("UUID")) {
                    openedBy.add(entry.getUUID("UUID"));
                }
            }
        }
        if (tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            net.minecraft.core.NonNullList<ItemStack> nlist = net.minecraft.core.NonNullList.withSize(
                    inventory.getContainerSize(), ItemStack.EMPTY);
            net.minecraft.world.ContainerHelper.loadAllItems(invTag, nlist, level().registryAccess());
            for (int i = 0; i < nlist.size(); i++) {
                inventory.setItem(i, nlist.get(i));
            }
        }
    }
}
