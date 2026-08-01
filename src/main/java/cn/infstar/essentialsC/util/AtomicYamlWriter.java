package cn.infstar.essentialsC.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicYamlWriter {

    private AtomicYamlWriter() {
    }

    public static void save(FileConfiguration configuration, File targetFile) throws IOException {
        Path target = targetFile.toPath().toAbsolutePath();
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String temporaryPrefix = targetFile.getName();
        if (temporaryPrefix.length() < 3) {
            temporaryPrefix = (temporaryPrefix + "___").substring(0, 3);
        }
        Path temporary = Files.createTempFile(parent, temporaryPrefix, ".tmp");
        try {
            Files.writeString(temporary, configuration.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
