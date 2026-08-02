package cn.infstar.essentialsC.admin;

import cn.infstar.essentialsC.util.AtomicYamlWriter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminModeStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesLegacyPlayersIntoIndependentFiles() throws Exception {
        UUID playerId = UUID.randomUUID();
        Path legacyFile = temporaryDirectory.resolve("admin-mode.yml");
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("players." + playerId + ".active", true);
        legacy.set("players." + playerId + ".normal.level", 27);
        AtomicYamlWriter.save(legacy, legacyFile.toFile());

        AdminModeStore store = new AdminModeStore(
            temporaryDirectory.resolve("admin-mode").toFile(), legacyFile.toFile(), Logger.getAnonymousLogger());

        YamlConfiguration migrated = store.load(playerId);
        assertNotNull(migrated);
        assertTrue(migrated.getBoolean("active"));
        assertEquals(27, migrated.getInt("normal.level"));
        assertTrue(Files.exists(temporaryDirectory.resolve("admin-mode").resolve(playerId + ".yml")));
        assertFalse(Files.exists(legacyFile));
        try (var backups = Files.list(temporaryDirectory)) {
            assertTrue(backups.anyMatch(path -> path.getFileName().toString().startsWith("admin-mode.legacy-")));
        }
    }
}
