package cn.infstar.essentialsC;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LangManager {

    private static final int CURRENT_CONFIG_VERSION = 2;

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration langFile;
    private String currentLanguage;

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadLanguage();
    }

    public String getPrefix() {
        return translateColorCodes(langFile.getString("prefix", "&6[EssentialsC] &r"));
    }

    public String getString(String path) {
        String value = langFile.getString(path);
        if (value == null) {
            return translateColorCodes("&cMissing translation: " + path);
        }
        return translateColorCodes(value);
    }

    public String getString(String path, Map<String, String> placeholders) {
        return applyPlaceholders(getString(path), placeholders);
    }

    public String getPrefixedString(String path) {
        return getPrefix() + getString(path);
    }

    public String getPrefixedString(String path, Map<String, String> placeholders) {
        return getPrefix() + getString(path, placeholders);
    }

    public List<String> getStringList(String path) {
        List<String> values = langFile.getStringList(path);
        if (values.isEmpty()) {
            values = List.of("&cMissing translation: " + path);
        }

        List<String> translated = new ArrayList<>();
        for (String value : values) {
            translated.add(translateColorCodes(value));
        }
        return translated;
    }

    public void reload() {
        loadConfig();
        loadLanguage();
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        migrateConfigIfNeeded(configFile);
        config = YamlConfiguration.loadConfiguration(configFile);
        config.addDefault("config-version", CURRENT_CONFIG_VERSION);
        config.addDefault("language", "zh_CN");
        config.options().copyDefaults(true);

        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save config.yml: " + e.getMessage());
        }
    }

    private void migrateConfigIfNeeded(File configFile) {
        FileConfiguration existingConfig = YamlConfiguration.loadConfiguration(configFile);
        int existingVersion = existingConfig.getInt("config-version", 0);
        if (existingVersion <= 0 || existingVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }

        String language = existingConfig.getString("language", "zh_CN");
        File backupFile = new File(plugin.getDataFolder(),
            "config.v" + existingVersion + ".bak-" + System.currentTimeMillis() + ".yml");

        try {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.saveResource("config.yml", true);

            FileConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);
            newConfig.set("language", language);
            newConfig.save(configFile);

            plugin.getLogger().info("Migrated config.yml from version " + existingVersion
                + " to " + CURRENT_CONFIG_VERSION + ". Backup saved to " + backupFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to migrate config.yml: " + e.getMessage());
        }
    }

    private void loadLanguage() {
        currentLanguage = config.getString("language", "zh_CN");

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create language folder: " + langFolder.getAbsolutePath());
        }

        File langFileObj = new File(langFolder, currentLanguage + ".yml");
        if (!langFileObj.exists()) {
            if (plugin.getResource("lang/" + currentLanguage + ".yml") != null) {
                plugin.saveResource("lang/" + currentLanguage + ".yml", false);
            } else {
                plugin.getLogger().warning("Language file not found: " + currentLanguage + ".yml, falling back to en_US");
                currentLanguage = "en_US";
                plugin.saveResource("lang/en_US.yml", false);
                langFileObj = new File(langFolder, "en_US.yml");
            }
        }

        langFile = YamlConfiguration.loadConfiguration(langFileObj);
        loadDefaultLanguageFallback();
    }

    private void loadDefaultLanguageFallback() {
        InputStream defaultLangStream = plugin.getResource("lang/en_US.yml");
        if (defaultLangStream == null) {
            return;
        }

        YamlConfiguration defaultLang = YamlConfiguration.loadConfiguration(
            new InputStreamReader(defaultLangStream, StandardCharsets.UTF_8)
        );
        langFile.setDefaults(defaultLang);
    }

    private String applyPlaceholders(String value, Map<String, String> placeholders) {
        String result = value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private String translateColorCodes(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }
}
