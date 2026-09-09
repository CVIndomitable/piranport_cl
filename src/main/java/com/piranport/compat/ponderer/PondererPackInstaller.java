package com.piranport.compat.ponderer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class PondererPackInstaller {
    static final String BUNDLED_RESOURCE = "/META-INF/resourcepacks/piranport-ponderer.zip";
    static final String PACK_PATH = "resourcepacks/[Ponderer] Piran Port.zip";
    static final String HASH_PATH = "config/piranport/ponderer-pack.sha256";

    enum Result {
        INSTALLED, UPDATED, UNCHANGED, PRESERVED_USER_FILE
    }

    private PondererPackInstaller() {}

    static Result installBundledPack(Path gameDirectory) throws IOException {
        try (InputStream input = PondererPackInstaller.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled Ponderer pack: " + BUNDLED_RESOURCE);
            }
            return install(gameDirectory, input.readAllBytes());
        }
    }

    static Result install(Path gameDirectory, byte[] bundledPack) throws IOException {
        Path target = gameDirectory.resolve(PACK_PATH);
        Path hashFile = gameDirectory.resolve(HASH_PATH);
        String bundledHash = sha256(bundledPack);
        boolean exists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);

        if (exists) {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                return Result.PRESERVED_USER_FILE;
            }
            String currentHash = sha256(Files.readAllBytes(target));
            if (currentHash.equals(bundledHash)) {
                saveHashIfChanged(hashFile, bundledHash);
                return Result.UNCHANGED;
            }
            if (!Files.isRegularFile(hashFile, LinkOption.NOFOLLOW_LINKS)
                    || !currentHash.equals(Files.readString(hashFile, StandardCharsets.UTF_8).strip())) {
                return Result.PRESERVED_USER_FILE;
            }
        }

        writeAtomically(target, bundledPack);
        saveHashIfChanged(hashFile, bundledHash);
        return exists ? Result.UPDATED : Result.INSTALLED;
    }

    private static void saveHashIfChanged(Path hashFile, String hash) throws IOException {
        if (Files.isRegularFile(hashFile, LinkOption.NOFOLLOW_LINKS)
                && hash.equals(Files.readString(hashFile, StandardCharsets.UTF_8).strip())) {
            return;
        }
        writeAtomically(hashFile, (hash + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Java runtime does not provide SHA-256", exception);
        }
    }

    private static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".piranport-ponderer-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
