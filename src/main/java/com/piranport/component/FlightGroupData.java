package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import io.netty.handler.codec.DecoderException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record FlightGroupData(List<FlightGroup> groups) {

    public static final int MAX_GROUPS = 4;

    public enum AttackMode implements StringRepresentable {
        FOCUS("focus"),
        SPREAD("spread"),
        FOLLOW("follow");

        private final String serializedName;

        AttackMode(String name) {
            this.serializedName = name;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public static final Codec<AttackMode> CODEC =
                StringRepresentable.fromEnum(AttackMode::values);

    // STREAM_CODEC removed for 1.20.1 (阶段7 NBT重写时处理)
    }

    public record FlightGroup(
            List<Integer> slotIndices,
            List<Integer> slotBulletsList,   // slot indices that have bullets enabled
            Map<Integer, String> slotPayload, // slot → "" | "piranport:aerial_torpedo" | "piranport:aerial_bomb"
            AttackMode attackMode,
            int sortOrder
    ) {
        private record SlotPayloadEntry(int slot, String payload) {}

        private static final Codec<SlotPayloadEntry> SLOT_PAYLOAD_CODEC = RecordCodecBuilder.create(i ->
                i.group(
                        Codec.INT.fieldOf("s").forGetter(SlotPayloadEntry::slot),
                        Codec.STRING.fieldOf("p").forGetter(SlotPayloadEntry::payload)
                ).apply(i, SlotPayloadEntry::new)
        );

        public static final Codec<FlightGroup> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.listOf().fieldOf("slot_indices").forGetter(FlightGroup::slotIndices),
                Codec.INT.listOf().optionalFieldOf("slot_bullets", List.of()).forGetter(FlightGroup::slotBulletsList),
                SLOT_PAYLOAD_CODEC.listOf().optionalFieldOf("slot_payloads", List.of())
                        .forGetter(g -> g.slotPayload().entrySet().stream()
                                .filter(e -> !e.getValue().isEmpty())
                                .map(e -> new SlotPayloadEntry(e.getKey(), e.getValue()))
                                .collect(Collectors.toList())),
                AttackMode.CODEC.fieldOf("attack_mode").forGetter(FlightGroup::attackMode),
                Codec.INT.fieldOf("sort_order").forGetter(FlightGroup::sortOrder)
        ).apply(i, (slots, bullets, payloads, mode, order) -> new FlightGroup(
                slots,
                bullets,
                payloads.stream().collect(Collectors.toMap(SlotPayloadEntry::slot, SlotPayloadEntry::payload)),
                mode,
                order
        )));

    // STREAM_CODEC removed for 1.20.1 (阶段7 NBT重写时处理)

        public static FlightGroup empty(int sortOrder) {
            return new FlightGroup(List.of(), List.of(), Map.of(), AttackMode.FOCUS, sortOrder);
        }

        /** Toggle a weapon slot's membership. Removing also clears its bullets and payload. */
        public FlightGroup withSlotToggled(int slotIndex) {
            List<Integer> newIndices = new ArrayList<>(slotIndices);
            List<Integer> newBullets = new ArrayList<>(slotBulletsList);
            Map<Integer, String> newPayload = new HashMap<>(slotPayload);
            if (newIndices.contains(slotIndex)) {
                newIndices.remove((Integer) slotIndex);
                newBullets.remove((Integer) slotIndex);
                newPayload.remove(slotIndex);
            } else {
                newIndices.add(slotIndex);
            }
            return new FlightGroup(List.copyOf(newIndices), List.copyOf(newBullets),
                    Map.copyOf(newPayload), attackMode, sortOrder);
        }

        /** Toggle bullets for a specific weapon slot. */
        public FlightGroup withSlotBulletsToggled(int slotIndex) {
            List<Integer> newBullets = new ArrayList<>(slotBulletsList);
            if (newBullets.contains(slotIndex)) {
                newBullets.remove((Integer) slotIndex);
            } else {
                newBullets.add(slotIndex);
            }
            return new FlightGroup(slotIndices, List.copyOf(newBullets), slotPayload, attackMode, sortOrder);
        }

        /** Set or clear the payload for a specific weapon slot. Empty string = no payload. */
        public FlightGroup withSlotPayload(int slotIndex, String payload) {
            Map<Integer, String> newPayload = new HashMap<>(slotPayload);
            if (payload.isEmpty()) {
                newPayload.remove(slotIndex);
            } else {
                newPayload.put(slotIndex, payload);
            }
            return new FlightGroup(slotIndices, slotBulletsList, Map.copyOf(newPayload), attackMode, sortOrder);
        }

        /** Returns true if this slot has bullets enabled. */
        public boolean getSlotBullets(int slotIndex) {
            return slotBulletsList.contains(slotIndex);
        }

        /** Returns the payload for this slot, or "" if none. */
        public String getSlotPayload(int slotIndex) {
            return slotPayload.getOrDefault(slotIndex, "");
        }

        public FlightGroup withAttackMode(AttackMode mode) {
            return new FlightGroup(slotIndices, slotBulletsList, slotPayload, mode, sortOrder);
        }
    }

    public static final Codec<FlightGroupData> CODEC = RecordCodecBuilder.create(i -> i.group(
            FlightGroup.CODEC.listOf().fieldOf("groups").forGetter(FlightGroupData::groups)
    ).apply(i, FlightGroupData::new));

    // STREAM_CODEC removed for 1.20.1 (阶段7 NBT重写时处理)

    public static FlightGroupData empty() {
        List<FlightGroup> groups = new ArrayList<>();
        for (int i = 0; i < MAX_GROUPS; i++) {
            groups.add(FlightGroup.empty(i + 1));
        }
        return new FlightGroupData(List.copyOf(groups));
    }

    public FlightGroupData withGroup(int index, FlightGroup group) {
        List<FlightGroup> newGroups = new ArrayList<>(groups);
        while (newGroups.size() <= index) {
            newGroups.add(FlightGroup.empty(newGroups.size() + 1));
        }
        newGroups.set(index, group);
        return new FlightGroupData(List.copyOf(newGroups));
    }
}
