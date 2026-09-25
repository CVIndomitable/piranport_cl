package com.piranport.component;

import com.piranport.aviation.AircraftDefinitionService;
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
        BombingMode bombingMode,
        boolean payloadLoaded,
        String definitionId
) {
    /** 兼容现有物品注册调用；迁移期按机种生成稳定 ID。 */
    public AircraftInfo(AircraftType aircraftType, int fuelCapacity, int ammoCapacity, int currentFuel,
                        float panelDamage, float panelSpeed, int weight, BombingMode bombingMode,
                        boolean payloadLoaded) {
        this(aircraftType, fuelCapacity, ammoCapacity, currentFuel, panelDamage, panelSpeed, weight,
                bombingMode, payloadLoaded, AircraftDefinitionService.legacyId(aircraftType));
    }

    private static final int MAX_CAPACITY = 32768;
    private static final float MIN_PANEL_DAMAGE = 0.0F;
    private static final float MAX_PANEL_DAMAGE = 100_000.0F;
    private static final float MIN_PANEL_SPEED = 0.05F;
    private static final float MAX_PANEL_SPEED = 100.0F;

    public AircraftInfo {
        fuelCapacity = Math.clamp(fuelCapacity, 1, MAX_CAPACITY);
        ammoCapacity = Math.clamp(ammoCapacity, 0, MAX_CAPACITY);
        currentFuel = Math.clamp(currentFuel, 0, fuelCapacity);
        panelDamage = Float.isFinite(panelDamage)
                ? Math.clamp(panelDamage, MIN_PANEL_DAMAGE, MAX_PANEL_DAMAGE) : MIN_PANEL_DAMAGE;
        panelSpeed = Float.isFinite(panelSpeed)
                ? Math.clamp(panelSpeed, MIN_PANEL_SPEED, MAX_PANEL_SPEED) : MIN_PANEL_SPEED;
        weight = Math.clamp(weight, 0, MAX_CAPACITY);
        definitionId = definitionId == null || definitionId.isBlank()
                ? AircraftDefinitionService.legacyId(aircraftType) : definitionId;
    }

    public enum AircraftType implements StringRepresentable {
        FIGHTER("fighter"),
        DIVE_BOMBER("dive_bomber"),
        TORPEDO_BOMBER("torpedo_bomber"),
        LEVEL_BOMBER("level_bomber"),
        ASW("asw"),
        RECON("recon"),
        ROCKET_FIGHTER("rocket_fighter");

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
            BombingMode.CODEC.optionalFieldOf("bombing_mode", BombingMode.DIVE).forGetter(AircraftInfo::bombingMode),
            // 对海挂载是否已装填（R 键手动装填模型）。旧存档缺省为 true，保持既有放飞体验
            Codec.BOOL.optionalFieldOf("payload_loaded", true).forGetter(AircraftInfo::payloadLoaded),
            // 空值由紧凑构造器按 aircraft_type 派生，避免旧鱼雷机存档被误判为战斗机定义。
            Codec.STRING.optionalFieldOf("definition_id", "")
                    .forGetter(AircraftInfo::definitionId)
    ).apply(i, AircraftInfo::new));

    private static final int STABLE_STREAM_MARKER = -1;

    /**
     * 网络格式使用稳定字符串 ID。解码器仍接受旧的首字段 ordinal，便于兼容窗口内读取旧客户端数据。
     */
    public static final StreamCodec<ByteBuf, AircraftInfo> STREAM_CODEC = StreamCodec.of(
            (buf, info) -> {
                ByteBufCodecs.VAR_INT.encode(buf, STABLE_STREAM_MARKER);
                ByteBufCodecs.STRING_UTF8.encode(buf, info.definitionId());
                AircraftType.STREAM_CODEC.encode(buf, info.aircraftType());
                ByteBufCodecs.VAR_INT.encode(buf, info.fuelCapacity());
                ByteBufCodecs.VAR_INT.encode(buf, info.ammoCapacity());
                ByteBufCodecs.VAR_INT.encode(buf, info.currentFuel());
                ByteBufCodecs.FLOAT.encode(buf, info.panelDamage());
                ByteBufCodecs.FLOAT.encode(buf, info.panelSpeed());
                ByteBufCodecs.VAR_INT.encode(buf, info.weight());
                BombingMode.STREAM_CODEC.encode(buf, info.bombingMode());
                buf.writeBoolean(info.payloadLoaded());
            },
            buf -> {
                int markerOrOrdinal = ByteBufCodecs.VAR_INT.decode(buf);
                String definitionId = null;
                AircraftType type;
                if (markerOrOrdinal == STABLE_STREAM_MARKER) {
                    definitionId = ByteBufCodecs.STRING_UTF8.decode(buf);
                    type = AircraftType.STREAM_CODEC.decode(buf);
                } else {
                    AircraftType[] values = AircraftType.values();
                    type = markerOrOrdinal >= 0 && markerOrOrdinal < values.length
                            ? values[markerOrOrdinal] : AircraftType.FIGHTER;
                }
                return new AircraftInfo(
                        type,
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        BombingMode.STREAM_CODEC.decode(buf),
                        buf.readBoolean(),
                        definitionId == null ? AircraftDefinitionService.legacyId(type) : definitionId
                );
            }
    );

    public AircraftInfo withCurrentFuel(int fuel) {
        return new AircraftInfo(aircraftType, fuelCapacity, ammoCapacity, fuel,
                panelDamage, panelSpeed, weight, bombingMode, payloadLoaded, definitionId);
    }

    public AircraftInfo withPayloadLoaded(boolean loaded) {
        return new AircraftInfo(aircraftType, fuelCapacity, ammoCapacity, currentFuel,
                panelDamage, panelSpeed, weight, bombingMode, loaded, definitionId);
    }
}
