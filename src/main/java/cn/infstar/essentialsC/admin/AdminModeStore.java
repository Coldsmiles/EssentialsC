package cn.infstar.essentialsC.admin;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.util.AtomicYamlWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Logger;

final class AdminModeStore {

    private final File directory;
    private final File legacyFile;
    private final Logger logger;

    AdminModeStore(EssentialsC plugin) {
        this(new File(plugin.getDataFolder(), "admin-mode"),
            new File(plugin.getDataFolder(), "admin-mode.yml"), plugin.getLogger());
    }

    AdminModeStore(File directory, File legacyFile, Logger logger) {
        this.directory = directory;
        this.legacyFile = legacyFile;
        this.logger = logger;
        migrateLegacyFile();
    }

    YamlConfiguration load(UUID playerId) {
        File playerFile = getPlayerFile(playerId);
        YamlConfiguration data = new YamlConfiguration();
        if (!playerFile.exists()) {
            return data;
        }
        try {
            data.load(playerFile);
        } catch (IOException | InvalidConfigurationException exception) {
            logger.warning("加载管理模式玩家存档失败 (" + playerId + "): " + exception.getMessage());
            return null;
        }
        return data;
    }

    boolean save(UUID playerId, YamlConfiguration data) {
        try {
            AtomicYamlWriter.save(data, getPlayerFile(playerId));
            return true;
        } catch (IOException exception) {
            logger.warning("保存管理模式玩家存档失败 (" + playerId + "): " + exception.getMessage());
            return false;
        }
    }

    private void migrateLegacyFile() {
        if (!legacyFile.exists()) {
            return;
        }

        YamlConfiguration legacy = new YamlConfiguration();
        try {
            legacy.load(legacyFile);
        } catch (IOException | InvalidConfigurationException exception) {
            logger.warning("加载旧 admin-mode.yml 失败，已保留原文件: " + exception.getMessage());
            return;
        }

        ConfigurationSection players = legacy.getConfigurationSection("players");
        if (players != null) {
            for (String playerIdText : players.getKeys(false)) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(playerIdText);
                } catch (IllegalArgumentException exception) {
                    logger.warning("忽略 admin-mode.yml 中无效的玩家 UUID: " + playerIdText);
                    continue;
                }

                if (getPlayerFile(playerId).exists()) {
                    continue;
                }
                YamlConfiguration migrated = new YamlConfiguration();
                copySection(players.getConfigurationSection(playerIdText), migrated);
                if (!save(playerId, migrated)) {
                    logger.warning("admin-mode.yml 迁移未完成，已保留原文件供下次重试。");
                    return;
                }
            }
        }

        File backup = new File(legacyFile.getParentFile(),
            "admin-mode.legacy-" + System.currentTimeMillis() + ".yml");
        try {
            Files.move(legacyFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("已将 admin-mode.yml 拆分为每玩家存档，旧文件已备份为 " + backup.getName());
        } catch (IOException exception) {
            logger.warning("备份旧 admin-mode.yml 失败: " + exception.getMessage());
        }
    }

    private void copySection(ConfigurationSection source, YamlConfiguration target) {
        if (source == null) {
            return;
        }
        for (String path : source.getKeys(true)) {
            if (!source.isConfigurationSection(path)) {
                target.set(path, source.get(path));
            }
        }
    }

    private File getPlayerFile(UUID playerId) {
        return new File(directory, playerId + ".yml");
    }
}
