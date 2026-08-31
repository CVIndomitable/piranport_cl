package com.piranport.dungeon.key;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores dungeon run progress on the key item's DataComponent.
 */
public record DungeonProgress(
        String currentNode,
        Set<String> clearedNodes,
        long startTimeMillis,
        boolean timerStarted
) {

    private static final int MAX_NODE_ID_LENGTH = 128;
    private static final int MAX_CLEARED_NODES = 256;
    public static final DungeonProgress EMPTY =
            new DungeonProgress("", Set.of(), 0L, false);

    public static final Codec<DungeonProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("current_node", "")
                    .forGetter(DungeonProgress::currentNode),
            Codec.STRING.listOf().xmap(l -> (Set<String>) new HashSet<>(l), l -> List.copyOf(l))
                    .optionalFieldOf("cleared_nodes", Set.of())
                    .forGetter(DungeonProgress::clearedNodes),
            Codec.LONG.optionalFieldOf("start_time_millis", 0L)
                    .forGetter(DungeonProgress::startTimeMillis),
            Codec.BOOL.optionalFieldOf("timer_started", false)
                    .forGetter(DungeonProgress::timerStarted)
    ).apply(i, DungeonProgress::new));

    public static final StreamCodec<ByteBuf, DungeonProgress> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, p.currentNode());
                ByteBufCodecs.VAR_INT.encode(buf, p.clearedNodes().size());
                for (String s : p.clearedNodes()) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, s);
                }
                buf.writeLong(p.startTimeMillis());
                ByteBufCodecs.BOOL.encode(buf, p.timerStarted());
            },
            buf -> {
                String currentNode = ByteBufCodecs.stringUtf8(MAX_NODE_ID_LENGTH).decode(buf);
                int size = ByteBufCodecs.VAR_INT.decode(buf);
                if (size < 0 || size > MAX_CLEARED_NODES) {
                    throw new DecoderException("cleared node count out of range: " + size);
                }
                Set<String> cleared = new HashSet<>(size);
                for (int i = 0; i < size; i++) {
                    String node = ByteBufCodecs.stringUtf8(MAX_NODE_ID_LENGTH).decode(buf);
                    cleared.add(node);
                }
                long startTime = buf.readLong();
                boolean timerStarted = ByteBufCodecs.BOOL.decode(buf);
                return new DungeonProgress(currentNode, Set.copyOf(cleared), startTime, timerStarted);
            }
    );

    public DungeonProgress withCurrentNode(String node) {
        return new DungeonProgress(node, clearedNodes, startTimeMillis, timerStarted);
    }

    public DungeonProgress withNodeCleared(String node) {
        Set<String> newCleared = new HashSet<>(clearedNodes);
        newCleared.add(node);
        return new DungeonProgress(currentNode, Set.copyOf(newCleared), startTimeMillis, timerStarted);
    }

    public DungeonProgress withTimerStarted(long startTime) {
        return new DungeonProgress(currentNode, clearedNodes, startTime, true);
    }
}
