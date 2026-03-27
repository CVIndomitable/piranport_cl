package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record FlightGroupData(List<FlightGroup> groups) {

    public static final int MAX_GROUPS = 4;

    public enum AttackMode implements StringRepresentable {
        FOCUS("focus"),
        SPREAD("spread");

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

        public static final StreamCodec<ByteBuf, AttackMode> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(i -> AttackMode.values()[i], Enum::ordinal);
    }

    public record FlightGroup(
            List<Integer> slotIndices,
            Map<Integer, String> slotAmmoTypes, // weapon slot index → ammo type id (empty = unassigned)
            AttackMode attackMode,
            int sortOrder
    ) {
        // Helper record for Codec serialization of each slot-ammo pair
        private record SlotAmmoEntry(int slot, String ammo) {}

        private static final Codec<SlotAmmoEntry> SLOT_AMMO_CODEC = RecordCodecBuilder.create(i ->
                i.group(
                        Codec.INT.fieldOf("s").forGetter(SlotAmmoEntry::slot),
                        Codec.STRING.fieldOf("a").forGetter(SlotAmmoEntry::ammo)
                ).apply(i, SlotAmmoEntry::new)
        );

        public static final Codec<FlightGroup> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.listOf().fieldOf("slot_indices").forGetter(FlightGroup::slotIndices),
                SLOT_AMMO_CODEC.listOf().fieldOf("slot_ammos")
                        .forGetter(g -> g.slotAmmoTypes().entrySet().stream()
                                .map(e -> new SlotAmmoEntry(e.getKey(), e.getValue()))
                                .collect(Collectors.toList())),
                AttackMode.CODEC.fieldOf("attack_mode").forGetter(FlightGroup::attackMode),
                Codec.INT.fieldOf("sort_order").forGetter(FlightGroup::sortOrder)
        ).apply(i, (slots, ammos, mode, order) -> new FlightGroup(
                slots,
                ammos.stream().collect(Collectors.toMap(SlotAmmoEntry::slot, SlotAmmoEntry::ammo)),
                mode,
                order
        )));

        public static final StreamCodec<ByteBuf, FlightGroup> STREAM_CODEC = StreamCodec.of(
                (buf, g) -> {
                    ByteBufCodecs.VAR_INT.encode(buf, g.slotIndices().size());
                    for (int idx : g.slotIndices()) ByteBufCodecs.VAR_INT.encode(buf, idx);
                    ByteBufCodecs.VAR_INT.encode(buf, g.slotAmmoTypes().size());
                    for (Map.Entry<Integer, String> e : g.slotAmmoTypes().entrySet()) {
                        ByteBufCodecs.VAR_INT.encode(buf, e.getKey());
                        ByteBufCodecs.STRING_UTF8.encode(buf, e.getValue());
                    }
                    AttackMode.STREAM_CODEC.encode(buf, g.attackMode());
                    ByteBufCodecs.VAR_INT.encode(buf, g.sortOrder());
                },
                buf -> {
                    int cnt = ByteBufCodecs.VAR_INT.decode(buf);
                    List<Integer> indices = new ArrayList<>(cnt);
                    for (int j = 0; j < cnt; j++) indices.add(ByteBufCodecs.VAR_INT.decode(buf));
                    int ammoCnt = ByteBufCodecs.VAR_INT.decode(buf);
                    Map<Integer, String> ammoMap = new HashMap<>(ammoCnt);
                    for (int j = 0; j < ammoCnt; j++) {
                        int slot = ByteBufCodecs.VAR_INT.decode(buf);
                        String ammo = ByteBufCodecs.STRING_UTF8.decode(buf);
                        ammoMap.put(slot, ammo);
                    }
                    AttackMode mode = AttackMode.STREAM_CODEC.decode(buf);
                    int order = ByteBufCodecs.VAR_INT.decode(buf);
                    return new FlightGroup(List.copyOf(indices), Map.copyOf(ammoMap), mode, order);
                }
        );

        public static FlightGroup empty(int sortOrder) {
            return new FlightGroup(List.of(), Map.of(), AttackMode.FOCUS, sortOrder);
        }

        /** Toggle a weapon slot's membership in this group. Removing a slot also clears its ammo. */
        public FlightGroup withSlotToggled(int slotIndex) {
            List<Integer> newIndices = new ArrayList<>(slotIndices);
            Map<Integer, String> newAmmo = new HashMap<>(slotAmmoTypes);
            if (newIndices.contains(slotIndex)) {
                newIndices.remove((Integer) slotIndex);
                newAmmo.remove(slotIndex);
            } else {
                newIndices.add(slotIndex);
                // ammo defaults to unassigned; don't add to map
            }
            return new FlightGroup(List.copyOf(newIndices), Map.copyOf(newAmmo), attackMode, sortOrder);
        }

        /** Set or clear the ammo type for a specific weapon slot. Empty string = unassigned. */
        public FlightGroup withSlotAmmo(int slotIndex, String ammo) {
            Map<Integer, String> newAmmo = new HashMap<>(slotAmmoTypes);
            if (ammo.isEmpty()) {
                newAmmo.remove(slotIndex);
            } else {
                newAmmo.put(slotIndex, ammo);
            }
            return new FlightGroup(slotIndices, Map.copyOf(newAmmo), attackMode, sortOrder);
        }

        /** Returns the ammo type assigned to this weapon slot, or "" if unassigned. */
        public String getSlotAmmo(int slotIndex) {
            return slotAmmoTypes.getOrDefault(slotIndex, "");
        }

        public FlightGroup withAttackMode(AttackMode mode) {
            return new FlightGroup(slotIndices, slotAmmoTypes, mode, sortOrder);
        }
    }

    public static final Codec<FlightGroupData> CODEC = RecordCodecBuilder.create(i -> i.group(
            FlightGroup.CODEC.listOf().fieldOf("groups").forGetter(FlightGroupData::groups)
    ).apply(i, FlightGroupData::new));

    public static final StreamCodec<ByteBuf, FlightGroupData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.VAR_INT.encode(buf, data.groups().size());
                for (FlightGroup g : data.groups()) FlightGroup.STREAM_CODEC.encode(buf, g);
            },
            buf -> {
                int size = ByteBufCodecs.VAR_INT.decode(buf);
                List<FlightGroup> groups = new ArrayList<>(size);
                for (int i = 0; i < size; i++) groups.add(FlightGroup.STREAM_CODEC.decode(buf));
                return new FlightGroupData(List.copyOf(groups));
            }
    );

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
