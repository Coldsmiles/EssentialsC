package cn.infstar.essentialsC.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicYamlWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsParentDirectoriesAndWritesUtf8Yaml() throws Exception {
        Path target = temporaryDirectory.resolve("nested/data.yml");
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("message", "中文内容");

        AtomicYamlWriter.save(configuration, target.toFile());

        assertTrue(Files.exists(target));
        assertTrue(Files.readString(target, StandardCharsets.UTF_8).contains("中文内容"));
        try (var files = Files.list(target.getParent())) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void replacesExistingYamlWithoutRetainingOldValues() throws Exception {
        Path target = temporaryDirectory.resolve("state.yml");
        YamlConfiguration initial = new YamlConfiguration();
        initial.set("old-value", true);
        AtomicYamlWriter.save(initial, target.toFile());

        YamlConfiguration replacement = new YamlConfiguration();
        replacement.set("new-value", 42);
        AtomicYamlWriter.save(replacement, target.toFile());

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(target.toFile());
        assertFalse(loaded.contains("old-value"));
        assertEquals(42, loaded.getInt("new-value"));
    }

    @Test
    void supportsShortTargetFileNames() throws Exception {
        Path target = temporaryDirectory.resolve("x");
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("written", true);

        AtomicYamlWriter.save(configuration, target.toFile());

        assertTrue(YamlConfiguration.loadConfiguration(target.toFile()).getBoolean("written"));
    }

    @Test
    void preservesParsedChineseComments() throws Exception {
        Path target = temporaryDirectory.resolve("commented.yml");
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.options().parseComments(true);
        configuration.loadFromString("# 中文配置注释\nenabled: false\n");
        configuration.set("enabled", true);

        AtomicYamlWriter.save(configuration, target.toFile());

        String saved = Files.readString(target, StandardCharsets.UTF_8);
        assertTrue(saved.contains("# 中文配置注释"));
        assertTrue(saved.contains("enabled: true"));
    }
}
