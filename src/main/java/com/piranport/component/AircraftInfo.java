package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public record AircraftInfo(
        AircraftType aircraftType,
        int fuelCapacity,
        int ammoCapacity,
        int currentFuel,
        float panelDamage,
        float panelSpeed,
        int weight,
        BombingMode bombingMode
) {
    public enum AircraftType implements StringRepresentable {
        FIGHTER("fighter"),
        DIVE_BOMBER("dive_bomber"),
        TORPEDO_BOMBER("torpedo_bomber"),
        LEVEL_BOMBER("level_bomber"),
        RECON("recon");

        private final String serializedName;

        AircraftType(String name) {
            this.serializedName = name;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public static final Codec<AircraftType> CODEC =
                StringRepresentable.fromEnum(AircraftType::values);

        public static final StreamCodec<ByteBuf, AircraftType> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(i -> {
                    AircraftType[] vals = AircraftType.values();
                    return (i >= 0 && i < vals.length) ? vals[i] : FIGHTER;
                }, Enum::ordinal);
    }

    /** 轰炸方式：水平轰炸（高空水平飞越投弹）或俯冲轰炸（爬升后俯冲接触投弹）。 */
    public enum BombingMode implements StringRepresentable {
        LEVEL("level"),
        DIVE("dive");

        private final String serializedName;

        BombingMode(String name) {
            this.serializedName = name;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public static final Codec<BombingMode> CODEC =
                StringRepresentable.fromEnum(BombingMode::values);

        public static final StreamCodec<ByteBuf, BombingMode> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(i -> {
                    BombingMode[] vals = BombingMode.values();
                    return (i >= 0 && i < vals.length) ? vals[i] : DIVE;
                }, Enum::ordinal);
    }

    public static final Codec<AircraftInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            AircraftType.CODEC.fieldOf("aircraft_type").forGetter(AircraftInfo::aircraftType),
            Codec.INT.fieldOf("fuel_capacity").forGetter(AircraftInfo::fuelCapacity),
            Codec.INT.fieldOf("ammo_capacity").forGetter(AircraftInfo::ammoCapacity),
            Codec.INT.fieldOf("current_fuel").forGetter(AircraftInfo::currentFuel),
            Codec.FLOAT.fieldOf("panel_damage").forGetter(AircraftInfo::panelDamage),
            Codec.FLOAT.fieldOf("panel_speed").forGetter(AircraftInfo::panelSpeed),
            Codec.INT.fieldOf("weight").forGetter(AircraftInfo::weight),
            BombingMode.CODEC.optionalFieldOf("bombing_mode", BombingMode.DIVE).forGetter(AircraftInfo::bombingMode)
    ).apply(i, AircraftInfo::new));

    public static final StreamCodec<ByteBuf, AircraftInfo> STREAM_CODEC = StreamCodec.of(
            (buf, info) -> {
                AircraftType.STREAM_CODEC.encode(buf, info.aircraftType());
                ByteBufCodecs.VAR_INT.encode(buf, info.fuelCapacity());
                ByteBufCodecs.VAR_INT.encode(buf, info.ammoCapacity());
                ByteBufCodecs.VAR_INT.encode(buf, info.currentFuel());
                ByteBufCodecs.FLOAT.encode(buf, info.panelDamage());
                ByteBufCodecs.FLOAT.encode(buf, info.panelSpeed());
                ByteBufCodecs.VAR_INT.encode(buf, info.weight());
                BombingMode.STREAM_CODEC.encode(buf, info.bombingMode());
            },
            buf -> new AircraftInfo(
                    AircraftType.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    BombingMode.STREAM_CODEC.decode(buf)
            )
    );

    public AircraftInfo withCurrentFuel(int fuel) {
        return new AircraftInfo(aircraftType, fuelCapacity, ammoCapacity, fuel,
                panelDamage, panelSpeed, weight, bombingMode);
    }
}
