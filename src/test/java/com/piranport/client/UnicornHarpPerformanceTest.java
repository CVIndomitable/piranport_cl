package com.piranport.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.piranport.network.HarpPerformancePayload;
import io.netty.buffer.Unpooled;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UnicornHarpPerformanceTest {
    @Test
    void pickerCrossesStringsOnThreeBeatsAndReturnsToRest() {
        assertEquals(0, UnicornHarpPerformance.envelope(0));
        for (int beat : new int[]{4, 8, 12}) {
            assertEquals(1, UnicornHarpPerformance.envelope(beat));
            assertEquals(0, UnicornHarpPerformance.sweep(beat), .001F);
            assertTrue(UnicornHarpPerformance.sweep(beat - 1) * UnicornHarpPerformance.sweep(beat + 1) < 0);
        }
        assertEquals(0, UnicornHarpPerformance.envelope(UnicornHarpPerformance.DURATION));
    }

    @Test
    void performancePacketPreservesThePlayingHand() {
        for (InteractionHand hand : InteractionHand.values()) {
            var packet = new HarpPerformancePayload(32765, hand);
            var buf = Unpooled.buffer();
            try {
                HarpPerformancePayload.STREAM_CODEC.encode(buf, packet);
                assertEquals(packet, HarpPerformancePayload.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes());
            } finally {
                buf.release();
            }
        }
    }

    @Test
    void onlyHandsUseTheSculptAndTheInventoryKeepsItsSprite() throws Exception {
        JsonObject m = model("unicorn_harp");
        assertEquals("neoforge:separate_transforms", m.get("loader").getAsString());
        JsonObject base = m.getAsJsonObject("base");
        assertEquals("minecraft:item/generated", base.get("parent").getAsString());
        assertEquals("piranport:item/unicorn_harp", base.getAsJsonObject("textures").get("layer0").getAsString());
        var hands = m.getAsJsonObject("perspectives");
        assertEquals(Set.of("firstperson_lefthand", "firstperson_righthand", "thirdperson_lefthand", "thirdperson_righthand"), hands.keySet());
        for (String hand : hands.keySet()) assertEquals("piranport:item/unicorn_harp_held", hands.getAsJsonObject(hand).get("parent").getAsString());
        var sculpt = model("unicorn_harp_held");
        assertFalse(sculpt.getAsJsonArray("elements").isEmpty());
        assertEquals("piranport:item/unicorn_harp_held", sculpt.getAsJsonObject("textures").get("skin").getAsString());
    }

    private JsonObject model(String name) throws Exception {
        try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("/assets/piranport/models/item/" + name + ".json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
