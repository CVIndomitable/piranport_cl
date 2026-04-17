package com.piranport.dungeon.script;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Base interface for scripted dungeon node sequences.
 *
 * Implementations must persist their full phase state via {@link #writeNbt} and a
 * matching factory registered in {@link DungeonScriptRegistry}, so that scripts
 * survive server restart (otherwise players would be stranded mid-dungeon).
 */
public interface DungeonScript {
    /** Called once per server tick. */
    void tick(ServerLevel dungeonLevel);

    /** Called when a tagged dungeon entity dies. */
    void onEntityDeath(Entity entity);

    /** Returns true when the script has completed and should be removed. */
    boolean isFinished();

    /** Stable type identifier matching the factory registered in {@link DungeonScriptRegistry}. */
    String typeId();

    /** Serialize phase state for persistence. */
    void writeNbt(CompoundTag tag);
}
