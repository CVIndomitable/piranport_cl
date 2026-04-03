package com.piranport.dungeon.script;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Base interface for scripted dungeon node sequences.
 */
public interface DungeonScript {
    /** Called once per server tick. */
    void tick(ServerLevel dungeonLevel);

    /** Called when a tagged dungeon entity dies. */
    void onEntityDeath(Entity entity);

    /** Returns true when the script has completed and should be removed. */
    boolean isFinished();
}
