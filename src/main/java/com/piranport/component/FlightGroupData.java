package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;

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
            String ammoType,
            AttackMode attackMode,
            int sortOrder
    ) {
        public static final Codec<FlightGroup> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.listOf().fieldOf("slot_indices").forGetter(FlightGroup::slotIndices),
                Codec.STRING.fieldOf("ammo_type").forGetter(FlightGroup::ammoType),
                AttackMode.CODEC.fieldOf("attack_mode").forGetter(FlightGroup::attackMode),
                Codec.INT.fieldOf("sort_order").forGetter(FlightGroup::sortOrder)
        ).apply(i, FlightGroup::new));

        public static final StreamCodec<ByteBuf, FlightGroup> STREAM_CODEC = StreamCodec.of(
                (buf, g) -> {
                    ByteBufCodecs.VAR_INT.encode(buf, g.slotIndices().size());
                    for (int idx : g.slotIndices()) ByteBufCodecs.VAR_INT.encode(buf, idx);
                    ByteBufCodecs.STRING_UTF8.encode(buf, g.ammoType());
                    AttackMode.STREAM_CODEC.encode(buf, g.attackMode());
                    ByteBufCodecs.VAR_INT.encode(buf, g.sortOrder());
                },
                buf -> {
                    int size = ByteBufCodecs.VAR_INT.decode(buf);
                    List<Integer> indices = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) indices.add(ByteBufCodecs.VAR_INT.decode(buf));
                    String ammo = ByteBufCodecs.STRING_UTF8.decode(buf);
                    AttackMode mode = AttackMode.STREAM_CODEC.decode(buf);
                    int order = ByteBufCodecs.VAR_INT.decode(buf);
                    return new FlightGroup(List.copyOf(indices), ammo, mode, order);
                }
        );

        public static FlightGroup empty(int sortOrder) {
            return new FlightGroup(List.of(), "", AttackMode.FOCUS, sortOrder);
        }

        public FlightGroup withSlotToggled(int slotIndex) {
            List<Integer> newIndices = new ArrayList<>(slotIndices);
            if (newIndices.contains(slotIndex)) {
                newIndices.remove((Integer) slotIndex);
            } else {
                newIndices.add(slotIndex);
            }
            return new FlightGroup(List.copyOf(newIndices), ammoType, attackMode, sortOrder);
        }

        public FlightGroup withAmmoType(String ammo) {
            return new FlightGroup(slotIndices, ammo, attackMode, sortOrder);
        }

        public FlightGroup withAttackMode(AttackMode mode) {
            return new FlightGroup(slotIndices, ammoType, mode, sortOrder);
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
