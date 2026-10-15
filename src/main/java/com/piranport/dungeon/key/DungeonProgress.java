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
 *
 * <p>enteredNodes 是后加字段：节点图 UI 需要区分「已进入未通关」（黄描边）与「未访问」（灰），
 * 仅靠 clearedNodes 表达不出来。老钥匙的 component 缺这个字段，codec 用 optionalFieldOf
 * 回落到空集；STREAM_CODEC 内部版本协商交给 NeoForge payload 框架，组件本身按新布局写读。</p>
 */
public record DungeonProgress(
        String currentNode,
        Set<String> clearedNodes,
        Set<String> enteredNodes,
        long elapsedMillis,
        boolean timerStarted
) {

    private static final int MAX_NODE_ID_LENGTH = 128;
    private static final int MAX_CLEARED_NODES = 256;
    private static final int MAX_ENTERED_NODES = 256;
    public static final DungeonProgress EMPTY =
            new DungeonProgress("", Set.of(), Set.of(), 0L, false);

    public static final Codec<DungeonProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("current_node", "")
                    .forGetter(DungeonProgress::currentNode),
            Codec.STRING.listOf().xmap(l -> (Set<String>) new HashSet<>(l), l -> List.copyOf(l))
                    .optionalFieldOf("cleared_nodes", Set.of())
                    .forGetter(DungeonProgress::clearedNodes),
            Codec.STRING.listOf().xmap(l -> (Set<String>) new HashSet<>(l), l -> List.copyOf(l))
                    .optionalFieldOf("entered_nodes", Set.of())
                    .forGetter(DungeonProgress::enteredNodes),
            Codec.LONG.optionalFieldOf("elapsed_millis", 0L)
                    .forGetter(DungeonProgress::elapsedMillis),
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
                ByteBufCodecs.VAR_INT.encode(buf, p.enteredNodes().size());
                for (String s : p.enteredNodes()) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, s);
                }
                buf.writeLong(p.elapsedMillis());
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
                int enteredSize = ByteBufCodecs.VAR_INT.decode(buf);
                if (enteredSize < 0 || enteredSize > MAX_ENTERED_NODES) {
                    throw new DecoderException("entered node count out of range: " + enteredSize);
                }
                Set<String> entered = new HashSet<>(enteredSize);
                for (int i = 0; i < enteredSize; i++) {
                    String node = ByteBufCodecs.stringUtf8(MAX_NODE_ID_LENGTH).decode(buf);
                    entered.add(node);
                }
                long elapsed = buf.readLong();
                boolean timerStarted = ByteBufCodecs.BOOL.decode(buf);
                return new DungeonProgress(currentNode, Set.copyOf(cleared),
                        Set.copyOf(entered), elapsed, timerStarted);
            }
    );

    public DungeonProgress withCurrentNode(String node) {
        return new DungeonProgress(node, clearedNodes, enteredNodes, elapsedMillis, timerStarted);
    }

    public DungeonProgress withNodeCleared(String node) {
        Set<String> newCleared = new HashSet<>(clearedNodes);
        newCleared.add(node);
        return new DungeonProgress(currentNode, Set.copyOf(newCleared), enteredNodes,
                elapsedMillis, timerStarted);
    }

    public DungeonProgress withTimerStarted(long elapsedMillis) {
        return new DungeonProgress(currentNode, clearedNodes, enteredNodes, elapsedMillis, true);
    }
}
