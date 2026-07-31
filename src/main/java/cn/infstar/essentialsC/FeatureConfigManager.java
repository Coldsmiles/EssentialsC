package cn.infstar.essentialsC;

import cn.infstar.essentialsC.util.AtomicYamlWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 管理独立功能配置，并负责迁移旧版配置结构。
 */
public final class FeatureConfigManager {

    private static final int MAIN_CONFIG_VERSION = 2;

    private final EssentialsC plugin;
    private final File blocksMenuFile;
    private FileConfiguration blocksMenuConfig;

    public FeatureConfigManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.blocksMenuFile = new File(plugin.getDataFolder(), "blocks-menu.yml");
        reload();
    }

    public void reload() {
        ensureResource(blocksMenuFile, "blocks-menu.yml");

        blocksMenuConfig = loadWithDefaults(blocksMenuFile, "blocks-menu.yml");
        migrateLegacyMainConfig();
        migrateLegacyDebugSettings();
        updateMainConfigVersion();
    }

    public FileConfiguration getBlocksMenuConfig() {
        return blocksMenuConfig;
    }

    public void saveBlocksMenuConfig() {
        save(blocksMenuConfig, blocksMenuFile);
    }

    private void migrateLegacyMainConfig() {
        FileConfiguration mainConfig = plugin.getConfig();
        boolean migrated = false;

        if (mainConfig.contains("blocks-menu", true)) {
            copySection(mainConfig.getConfigurationSection("blocks-menu"), blocksMenuConfig);
            mainConfig.set("blocks-menu", null);
            migrated = true;
        }

        if (!migrated) {
            return;
        }

        mainConfig.set("config-version", MAIN_CONFIG_VERSION);
        saveMainConfig();
        plugin.getLogger().info("已将便捷菜单配置迁移到 blocks-menu.yml。");
    }

    private void migrateLegacyDebugSettings() {
        FileConfiguration mainConfig = plugin.getConfig();
        boolean hasJeiDebug = mainConfig.contains("jei-sync.debug", true);
        boolean hasSkinBridgeDebug = mainConfig.contains("skin-bridge.debug", true);
        if (!hasJeiDebug && !hasSkinBridgeDebug) {
            return;
        }

        boolean debugEnabled = mainConfig.getBoolean("debug", false)
            || mainConfig.getBoolean("jei-sync.debug", false)
            || mainConfig.getBoolean("skin-bridge.debug", false);
        mainConfig.set("debug", debugEnabled);
        mainConfig.set("jei-sync.debug", null);
        mainConfig.set("skin-bridge.debug", null);
        saveMainConfig();
        plugin.getLogger().info("已将独立功能调试开关合并到 config.yml 的全局 debug。");
    }

    private void updateMainConfigVersion() {
        if (plugin.getConfig().getInt("config-version", 0) >= MAIN_CONFIG_VERSION) {
            return;
        }
        plugin.getConfig().set("config-version", MAIN_CONFIG_VERSION);
        saveMainConfig();
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
            AtomicYamlWriter.save(config, file);
        } catch (Exception exception) {
            plugin.getLogger().warning("保存 " + file.getName() + " 失败: " + exception.getMessage());
        }
    }

    private void saveMainConfig() {
        save(plugin.getConfig(), new File(plugin.getDataFolder(), "config.yml"));
    }
}
