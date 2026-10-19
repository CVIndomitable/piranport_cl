package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 飞机物品上的运行时状态。
 *
 * <p>静态数值（容量、伤害、速度、重量、生命、冷却）全部来自
 * {@link com.piranport.aviation.AircraftDefinition}，这里不再复制一份。
 * 本组件只保存会随玩家操作变化的三项：当前燃油、对海挂载是否已装填、装填的是否为核航弹。
 *
 * <p>{@code nuclearPayload} 必须随装填一起落到物品状态上：核航弹只允许三架水平轰炸机挂载
 * （副本/08），而放飞后的实体只拿得到这份状态快照，不能再回头扫描背包判断。
 */
public record AircraftState(int currentFuel, boolean payloadLoaded, boolean nuclearPayload) {
    public static final AircraftState EMPTY = new AircraftState(0, false, false);
    private static final int MAX_FUEL = 32768;

    public AircraftState {
        currentFuel = Math.clamp(currentFuel, 0, MAX_FUEL);
    }

    public static final Codec<AircraftState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("current_fuel", 0).forGetter(AircraftState::currentFuel),
            Codec.BOOL.optionalFieldOf("payload_loaded", false).forGetter(AircraftState::payloadLoaded),
            Codec.BOOL.optionalFieldOf("nuclear_payload", false).forGetter(AircraftState::nuclearPayload)
    ).apply(instance, AircraftState::new));

    public static final StreamCodec<ByteBuf, AircraftState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AircraftState::currentFuel,
            ByteBufCodecs.BOOL, AircraftState::payloadLoaded,
            ByteBufCodecs.BOOL, AircraftState::nuclearPayload,
            AircraftState::new);

    public AircraftState withCurrentFuel(int fuel) {
        return new AircraftState(fuel, payloadLoaded, nuclearPayload);
    }

    public AircraftState withPayloadLoaded(boolean loaded) {
        return new AircraftState(currentFuel, loaded, loaded && nuclearPayload);
    }

    /** 装填/清空时同时记录挂载类型；清空挂载必须顺带清掉核弹标记。 */
    public AircraftState withPayload(boolean loaded, boolean nuclear) {
        return new AircraftState(currentFuel, loaded, loaded && nuclear);
    }
}
