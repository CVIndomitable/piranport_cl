package com.piranport.dungeon.script;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Maps script type IDs to factories that rebuild a {@link DungeonScript} from NBT.
 * Used by {@link DungeonScriptManager} when loading SavedData on server start.
 */
public final class DungeonScriptRegistry {
    private DungeonScriptRegistry() {}

    private static final Map<String, Function<CompoundTag, DungeonScript>> FACTORIES = new HashMap<>();

    public static void register(String typeId, Function<CompoundTag, DungeonScript> factory) {
        FACTORIES.put(typeId, factory);
    }

    public static DungeonScript create(String typeId, CompoundTag tag) {
        Function<CompoundTag, DungeonScript> f = FACTORIES.get(typeId);
        return f != null ? f.apply(tag) : null;
    }

    static {
        register(ArtilleryIntroScript.TYPE_ID, ArtilleryIntroScript::loadFromNbt);
    }
}
