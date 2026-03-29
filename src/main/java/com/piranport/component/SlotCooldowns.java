package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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

    public static final StreamCodec<ByteBuf, SlotCooldowns> STREAM_CODEC = StreamCodec.of(
            (buf, sc) -> {
                // encode endTick map
                ByteBufCodecs.VAR_INT.encode(buf, sc.endTick().size());
                for (Map.Entry<Integer, Long> e : sc.endTick().entrySet()) {
                    ByteBufCodecs.VAR_INT.encode(buf, e.getKey());
                    buf.writeLong(e.getValue());
                }
                // encode totalTick map
                ByteBufCodecs.VAR_INT.encode(buf, sc.totalTick().size());
                for (Map.Entry<Integer, Integer> e : sc.totalTick().entrySet()) {
                    ByteBufCodecs.VAR_INT.encode(buf, e.getKey());
                    ByteBufCodecs.VAR_INT.encode(buf, e.getValue());
                }
            },
            buf -> {
                int eCnt = ByteBufCodecs.VAR_INT.decode(buf);
                Map<Integer, Long> endMap = new HashMap<>(eCnt);
                for (int i = 0; i < eCnt; i++) {
                    int slot = ByteBufCodecs.VAR_INT.decode(buf);
                    long tick = buf.readLong();
                    endMap.put(slot, tick);
                }
                int tCnt = ByteBufCodecs.VAR_INT.decode(buf);
                Map<Integer, Integer> totalMap = new HashMap<>(tCnt);
                for (int i = 0; i < tCnt; i++) {
                    int slot = ByteBufCodecs.VAR_INT.decode(buf);
                    int total = ByteBufCodecs.VAR_INT.decode(buf);
                    totalMap.put(slot, total);
                }
                return new SlotCooldowns(Map.copyOf(endMap), Map.copyOf(totalMap));
            }
    );

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
        Map<Integer, Long> newEnd = new HashMap<>(endTick);
        Map<Integer, Integer> newTotal = new HashMap<>(totalTick);
        newEnd.put(slot, currentTick + ticks);
        newTotal.put(slot, ticks);
        return new SlotCooldowns(Map.copyOf(newEnd), Map.copyOf(newTotal));
    }
}
