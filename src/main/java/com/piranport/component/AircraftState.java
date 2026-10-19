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
 * 本组件只保存会随玩家操作变化的两项：当前燃油与对海挂载是否已装填。
 */
public record AircraftState(int currentFuel, boolean payloadLoaded) {
    public static final AircraftState EMPTY = new AircraftState(0, false);
    private static final int MAX_FUEL = 32768;

    public AircraftState {
        currentFuel = Math.clamp(currentFuel, 0, MAX_FUEL);
    }

    public static final Codec<AircraftState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("current_fuel", 0).forGetter(AircraftState::currentFuel),
            Codec.BOOL.optionalFieldOf("payload_loaded", false).forGetter(AircraftState::payloadLoaded)
    ).apply(instance, AircraftState::new));

    public static final StreamCodec<ByteBuf, AircraftState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AircraftState::currentFuel,
            ByteBufCodecs.BOOL, AircraftState::payloadLoaded,
            AircraftState::new);

    public AircraftState withCurrentFuel(int fuel) {
        return new AircraftState(fuel, payloadLoaded);
    }

    public AircraftState withPayloadLoaded(boolean loaded) {
        return new AircraftState(currentFuel, loaded);
    }
}
