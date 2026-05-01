package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-weapon-slot cooldown tracking stored on the ship core DataComponent.
 * endTick: slot index → absolute server game tick when the slot becomes ready
 * totalTick: slot index → total cooldown duration (for computing display fraction)
 */
public record SlotCooldowns(Map<Integer, Long> endTick, Map<Integer, Integer> totalTick) {

    public static final SlotCooldowns EMPTY = new SlotCooldowns(Map.of(), Map.of());

    private static final Codec<Map<Integer, Long>> INT_LONG_MAP = Codec.unboundedMap(
            Codec.STRING.xmap(Integer::parseInt, String::valueOf), Codec.LONG);
    private static final Codec<Map<Integer, Integer>> INT_INT_MAP = Codec.unboundedMap(
            Codec.STRING.xmap(Integer::parseInt, String::valueOf), Codec.INT);

    public static final Codec<SlotCooldowns> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            INT_LONG_MAP.optionalFieldOf("end_tick", Map.of()).forGetter(SlotCooldowns::endTick),
            INT_INT_MAP.optionalFieldOf("total_tick", Map.of()).forGetter(SlotCooldowns::totalTick)
    ).apply(inst, SlotCooldowns::new));

    // STREAM_CODEC removed for 1.20.1 (阶段7 NBT重写时处理)

    /** Returns true if this slot is still cooling down at the given game tick. */
    public boolean isOnCooldown(int slot, long currentTick) {
        return endTick.getOrDefault(slot, 0L) > currentTick;
    }

    /**
     * Returns cooldown progress fraction [0, 1]:
     * 0 = ready (no cooldown), 1 = just fired (full cooldown remaining).
     */
    public float getFraction(int slot, long currentTick) {
        long end = endTick.getOrDefault(slot, 0L);
        int total = totalTick.getOrDefault(slot, 1);
        if (end <= currentTick) return 0f;
        return Math.min(1f, (float) (end - currentTick) / total);
    }

    /** Returns a new SlotCooldowns with the given slot's cooldown set. */
    public SlotCooldowns withSlotCooldown(int slot, int ticks, long currentTick) {
        int adjusted = com.piranport.debug.PiranPortDebug.applyCooldownOverride(ticks);
        Map<Integer, Long> newEnd = new HashMap<>(endTick);
        Map<Integer, Integer> newTotal = new HashMap<>(totalTick);
        newEnd.put(slot, currentTick + adjusted);
        newTotal.put(slot, adjusted);
        return new SlotCooldowns(Map.copyOf(newEnd), Map.copyOf(newTotal));
    }
}
