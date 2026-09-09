package com.piranport.compat.ponderer;

import com.nododiiiii.ponderer.ponder.PonderPackInfo;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static com.piranport.compat.ponderer.PondererPackInstaller.Result.*;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.*;

class PondererPackInstallerTest {
    private static final byte[] PREVIOUS_BUNDLE = "previous bundled ZIP".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEXT_BUNDLE = "next bundled ZIP".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path gameDirectory;

    @Test
    void freshInstallUsesEmbeddedPackAndIsAcceptedByPonderer() throws IOException {
        assertEquals(INSTALLED, PondererPackInstaller.installBundledPack(gameDirectory));
        assertNotNull(PonderPackInfo.fromZip(packPath()));
        assertArrayEquals(Files.readAllBytes(Path.of(System.getProperty("piranport.pondererPack"))),
                Files.readAllBytes(packPath()));
        assertTrue(Files.readString(hashPath()).strip().matches("[a-f0-9]{64}"));
    }

    @Test
    void repeatLaunchDoesNotRewritePackOrHash() throws IOException {
        PondererPackInstaller.installBundledPack(gameDirectory);
        FileTime originalTime = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(packPath(), originalTime);
        Files.setLastModifiedTime(hashPath(), originalTime);

        assertEquals(UNCHANGED, PondererPackInstaller.installBundledPack(gameDirectory));
        assertEquals(originalTime, Files.getLastModifiedTime(packPath()));
        assertEquals(originalTime, Files.getLastModifiedTime(hashPath()));
    }

    @Test
    void unchangedManagedPackUpgradesAndRecordsNewHash() throws IOException {
        PondererPackInstaller.install(gameDirectory, PREVIOUS_BUNDLE);
        String previousHash = Files.readString(hashPath());

        assertEquals(UPDATED, PondererPackInstaller.install(gameDirectory, NEXT_BUNDLE));
        assertArrayEquals(NEXT_BUNDLE, Files.readAllBytes(packPath()));
        assertNotEquals(previousHash, Files.readString(hashPath()));
        assertEquals(UNCHANGED, PondererPackInstaller.install(gameDirectory, NEXT_BUNDLE));
    }

    @Test
    void identicalManualInstallIsAdoptedWithoutRewritingIt() throws IOException {
        Files.createDirectories(packPath().getParent());
        Files.write(packPath(), PREVIOUS_BUNDLE);
        FileTime originalTime = Files.getLastModifiedTime(packPath());

        assertEquals(UNCHANGED, PondererPackInstaller.install(gameDirectory, PREVIOUS_BUNDLE));
        assertEquals(originalTime, Files.getLastModifiedTime(packPath()));
        assertTrue(Files.exists(hashPath()));
        assertEquals(UPDATED, PondererPackInstaller.install(gameDirectory, NEXT_BUNDLE));
    }

    @Test
    void differentUntrackedPackIsNotOverwrittenOrAdopted() throws IOException {
        Files.createDirectories(packPath().getParent());
        Files.write(packPath(), PREVIOUS_BUNDLE);

        assertEquals(PRESERVED_USER_FILE, PondererPackInstaller.install(gameDirectory, NEXT_BUNDLE));
        assertArrayEquals(PREVIOUS_BUNDLE, Files.readAllBytes(packPath()));
        assertFalse(Files.exists(hashPath()));
    }

    @Test
    void userEditedManagedPackAndOwnershipHashArePreserved() throws IOException {
        PondererPackInstaller.install(gameDirectory, PREVIOUS_BUNDLE);
        String previousHash = Files.readString(hashPath());
        Files.writeString(packPath(), "user edited ZIP");

        assertEquals(PRESERVED_USER_FILE, PondererPackInstaller.install(gameDirectory, NEXT_BUNDLE));
        assertEquals("user edited ZIP", Files.readString(packPath()));
        assertEquals(previousHash, Files.readString(hashPath()));
    }

    @Test
    void removedManagedPackIsReinstalled() throws IOException {
        PondererPackInstaller.install(gameDirectory, PREVIOUS_BUNDLE);
        Files.delete(packPath());

        assertEquals(INSTALLED, PondererPackInstaller.install(gameDirectory, NEXT_BUNDLE));
        assertArrayEquals(NEXT_BUNDLE, Files.readAllBytes(packPath()));
    }

    @Test
    void directoryAtDestinationIsPreserved() throws IOException {
        Files.createDirectories(packPath());
        assertEquals(PRESERVED_USER_FILE, PondererPackInstaller.install(gameDirectory, NEXT_BUNDLE));
        assertTrue(Files.isDirectory(packPath()));
    }

    @Test
    void symlinkAtDestinationIsPreserved() throws IOException {
        Path userPack = gameDirectory.resolve("custom.zip");
        Files.write(userPack, PREVIOUS_BUNDLE);
        Files.createDirectories(packPath().getParent());
        Files.createSymbolicLink(packPath(), userPack);

        assertEquals(PRESERVED_USER_FILE, PondererPackInstaller.install(gameDirectory, NEXT_BUNDLE));
        assertTrue(Files.isSymbolicLink(packPath()));
        assertArrayEquals(PREVIOUS_BUNDLE, Files.readAllBytes(userPack));
    }

    @Test
    void installerIsClientOnlyAndDoesNotLinkToOptionalModClasses() {
        EventBusSubscriber subscriber = PondererClientCompat.class.getAnnotation(EventBusSubscriber.class);
        assertNotNull(subscriber);
        assertArrayEquals(new Dist[]{Dist.CLIENT}, subscriber.value());
        assertEquals(EventBusSubscriber.Bus.MOD, subscriber.bus());

        noClasses().should().dependOnClassesThat()
                .resideInAnyPackage("com.nododiiiii..", "net.createmod..")
                .check(new ClassFileImporter().importClasses(PondererClientCompat.class, PondererPackInstaller.class));
    }

    private Path packPath() {
        return gameDirectory.resolve(PondererPackInstaller.PACK_PATH);
    }

    private Path hashPath() {
        return gameDirectory.resolve(PondererPackInstaller.HASH_PATH);
    }
}
