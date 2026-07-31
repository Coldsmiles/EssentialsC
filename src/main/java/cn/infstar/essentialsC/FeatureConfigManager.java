package cn.infstar.essentialsC;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 管理体积较大的独立功能配置，并负责从旧版 config.yml 迁移数据。
 */
public final class FeatureConfigManager {

    private static final int MAIN_CONFIG_VERSION = 2;

    private final EssentialsC plugin;
    private final File skinBridgeFile;
    private final File blocksMenuFile;
    private FileConfiguration skinBridgeConfig;
    private FileConfiguration blocksMenuConfig;

    public FeatureConfigManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.skinBridgeFile = new File(plugin.getDataFolder(), "skin-bridge.yml");
        this.blocksMenuFile = new File(plugin.getDataFolder(), "blocks-menu.yml");
        reload();
    }

    public void reload() {
        ensureResource(skinBridgeFile, "skin-bridge.yml");
        ensureResource(blocksMenuFile, "blocks-menu.yml");

        skinBridgeConfig = loadWithDefaults(skinBridgeFile, "skin-bridge.yml");
        blocksMenuConfig = loadWithDefaults(blocksMenuFile, "blocks-menu.yml");
        migrateLegacyMainConfig();
        migrateLegacyDebugSettings();
        updateMainConfigVersion();
        saveSkinBridgeConfig();
        saveBlocksMenuConfig();
    }

    public FileConfiguration getSkinBridgeConfig() {
        return skinBridgeConfig;
    }

    public FileConfiguration getBlocksMenuConfig() {
        return blocksMenuConfig;
    }

    public void saveSkinBridgeConfig() {
        save(skinBridgeConfig, skinBridgeFile);
    }

    public void saveBlocksMenuConfig() {
        save(blocksMenuConfig, blocksMenuFile);
    }

    private void migrateLegacyMainConfig() {
        FileConfiguration mainConfig = plugin.getConfig();
        boolean migrated = false;

        if (mainConfig.contains("skin-bridge", true)) {
            copySection(mainConfig.getConfigurationSection("skin-bridge"), skinBridgeConfig);
            mainConfig.set("skin-bridge", null);
            migrated = true;
        }
        if (mainConfig.contains("blocks-menu", true)) {
            copySection(mainConfig.getConfigurationSection("blocks-menu"), blocksMenuConfig);
            mainConfig.set("blocks-menu", null);
            migrated = true;
        }

        if (!migrated) {
            return;
        }

        mainConfig.set("config-version", MAIN_CONFIG_VERSION);
        plugin.saveConfig();
        plugin.getLogger().info("已将 SkinBridge 与便捷菜单配置迁移到独立配置文件。");
    }

    private void migrateLegacyDebugSettings() {
        FileConfiguration mainConfig = plugin.getConfig();
        boolean hasJeiDebug = mainConfig.contains("jei-sync.debug", true);
        boolean hasSkinBridgeDebug = skinBridgeConfig.contains("debug", true);
        if (!hasJeiDebug && !hasSkinBridgeDebug) {
            return;
        }

        boolean debugEnabled = mainConfig.getBoolean("debug", false)
            || mainConfig.getBoolean("jei-sync.debug", false)
            || skinBridgeConfig.getBoolean("debug", false);
        mainConfig.set("debug", debugEnabled);
        mainConfig.set("jei-sync.debug", null);
        skinBridgeConfig.set("debug", null);
        plugin.saveConfig();
        plugin.getLogger().info("已将独立功能调试开关合并到 config.yml 的全局 debug。");
    }

    private void updateMainConfigVersion() {
        if (plugin.getConfig().getInt("config-version", 0) >= MAIN_CONFIG_VERSION) {
            return;
        }
        plugin.getConfig().set("config-version", MAIN_CONFIG_VERSION);
        plugin.saveConfig();
    }

    private void copySection(ConfigurationSection source, FileConfiguration target) {
        if (source == null) {
            return;
        }
        for (String path : source.getKeys(true)) {
            if (!source.isConfigurationSection(path)) {
                target.set(path, source.get(path));
            }
        }
    }

    private FileConfiguration loadWithDefaults(File file, String resourcePath) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        InputStream defaultsStream = plugin.getResource(resourcePath);
        if (defaultsStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultsStream, StandardCharsets.UTF_8)
            );
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
        }
        return config;
    }

    private void ensureResource(File file, String resourcePath) {
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private void save(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存 " + file.getName() + " 失败: " + exception.getMessage());
        }
    }
}
