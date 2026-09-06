package com.piranport.compat.ponderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.nododiiiii.ponderer.ponder.DslScene;
import com.nododiiiii.ponderer.ponder.LocalizedText;
import com.nododiiiii.ponderer.ponder.PonderPackInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PondererPackTest {
    private static final Path PACK = Path.of(System.getProperty("piranport.pondererPack"));
    private static final Path SOURCE = Path.of(System.getProperty("piranport.pondererSource"));
    private static final String SCRIPT = "data/ponderer/scripts/piranport_processing.json";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalizedText.class, new LocalizedText.GsonAdapter())
            .create();

    @TempDir
    Path temporaryDirectory;

    @Test
    void installedPondererRejectsOriginalUppercasePackName() throws IOException {
        Path invalidPack = temporaryDirectory.resolve("invalid.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(invalidPack))) {
            output.putNextEntry(new ZipEntry("pack.json"));
            output.write("{\"ponderer\":{\"name\":\"PiranPort\"}}".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        assertNull(PonderPackInfo.fromZip(invalidPack));
    }

    @Test
    void installedPondererAcceptsPackMetadata() {
        PonderPackInfo info = PonderPackInfo.fromZip(PACK);
        assertNotNull(info, "Ponderer must accept the actual distributed ZIP");
        assertEquals("piranport", info.name);
        assertEquals("[piranport]", info.packPrefix);
    }

    @Test
    void archiveContainsCurrentSourcesAndMinecraft1211Metadata() throws IOException {
        try (ZipFile archive = new ZipFile(PACK.toFile())) {
            Set<String> expectedFiles = Set.of("pack.json", "pack.mcmeta", SCRIPT);
            assertEquals(expectedFiles, archive.stream().filter(entry -> !entry.isDirectory())
                    .map(ZipEntry::getName).collect(java.util.stream.Collectors.toSet()));
            for (String name : expectedFiles) {
                try (var input = archive.getInputStream(archive.getEntry(name))) {
                    assertArrayEquals(Files.readAllBytes(SOURCE.resolve(name)), input.readAllBytes(), name);
                }
            }
            try (var reader = new InputStreamReader(archive.getInputStream(archive.getEntry("pack.mcmeta")),
                    StandardCharsets.UTF_8)) {
                JsonObject metadata = GSON.fromJson(reader, JsonObject.class);
                assertEquals(34, metadata.getAsJsonObject("pack").get("pack_format").getAsInt());
            }
        }
    }

    @Test
    void pondererParsesAllThreeTutorialSegments() throws IOException {
        DslScene scene = readScene();
        assertEquals("piranport:processing", scene.id);
        assertEquals(Set.of("piranport:stone_mill", "piranport:cooking_pot", "piranport:cutting_board"),
                Set.copyOf(scene.items));
        assertEquals(List.of("stone_mill", "cooking_pot", "cutting_board"),
                scene.scenes.stream().map(segment -> segment.id).toList());
        assertLocalized(scene.title);
        for (DslScene.SceneSegment segment : scene.scenes) {
            assertLocalized(segment.title);
            DslScene.DslStep firstStep = segment.steps.getFirst();
            assertEquals("show_structure", firstStep.type, segment.id);
            assertEquals("ponderer:basic", firstStep.structure, segment.id);
            assertTrue(segment.steps.stream().anyMatch(step -> "set_block".equals(step.type)
                    && ("piranport:" + segment.id).equals(step.block)), segment.id);
            for (DslScene.DslStep step : segment.steps) {
                if ("text".equals(step.type)) {
                    assertLocalized(step.text);
                }
            }
        }
        try (var structure = DslScene.class.getResourceAsStream("/data/ponderer/default_structures/basic.nbt")) {
            assertNotNull(structure, "The selected background must exist in the tested Ponderer version");
        }
    }

    @Test
    void textOverlaysHaveTimeToFinishBeforeNextCaptionAndSceneEnd() throws IOException {
        for (DslScene.SceneSegment segment : readScene().scenes) {
            int elapsedTicks = 0;
            int textEndTick = 0;
            for (DslScene.DslStep step : segment.steps) {
                if ("idle".equals(step.type)) {
                    elapsedTicks += step.durationOrDefault(20);
                } else if ("text".equals(step.type)) {
                    assertTrue(elapsedTicks >= textEndTick, segment.id + " has overlapping captions");
                    textEndTick = elapsedTicks + step.durationOrDefault(60);
                }
            }
            assertTrue(elapsedTicks >= textEndTick, segment.id + " ends before its final caption");
        }
    }

    private static DslScene readScene() throws IOException {
        try (ZipFile archive = new ZipFile(PACK.toFile());
             var reader = new InputStreamReader(archive.getInputStream(archive.getEntry(SCRIPT)),
                     StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, DslScene.class);
        }
    }

    private static void assertLocalized(LocalizedText text) {
        assertNotNull(text);
        for (String language : List.of("zh_cn", "en_us")) {
            String translation = text.getExact(language);
            assertNotNull(translation, language);
            assertFalse(translation.isBlank(), language);
        }
    }
}
