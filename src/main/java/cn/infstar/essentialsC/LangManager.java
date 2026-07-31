package cn.infstar.essentialsC;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LangManager {

    private static final int CURRENT_CONFIG_VERSION = 3;
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Map<String, String> THEME_COLORS = Map.of(
        "&a", "&#00fb9a",
        "&b", "&#5286ff",
        "&c", "&#ff7e5e",
        "&d", "&#c160ff",
        "&e", "&#ffc43b",
        "&6", "&#f5c962"
    );

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
        return renderToLegacy(langFile.getString("prefix", "&6[EssentialsC] &r"));
    }

    public String getString(String path) {
        String value = langFile.getString(path);
        if (value == null) {
            return translateColorCodes("&c缺少语言文本: " + path);
        }
        return renderToLegacy(value);
    }

    public String getString(String path, Map<String, String> placeholders) {
        String value = langFile.getString(path);
        if (value == null) {
            return translateColorCodes("&c缺少语言文本: " + path);
        }
        return renderToLegacy(applyPlaceholders(value, placeholders));
    }

    public Component getComponent(String path) {
        return getComponent(path, Map.of());
    }

    public Component getComponent(String path, Map<String, String> placeholders) {
        String value = langFile.getString(path);
        if (value == null) {
            return LegacyComponentSerializer.legacySection()
                .deserialize(translateColorCodes("&c缺少语言文本: " + path));
        }

        return renderToComponent(applyPlaceholders(value, placeholders));
    }

    public Component getPrefixedComponent(String path) {
        return getPrefixedComponent(path, Map.of());
    }

    public Component getPrefixedComponent(String path, Map<String, String> placeholders) {
        String value = langFile.getString(path);
        if (value == null) {
            return LegacyComponentSerializer.legacySection()
                .deserialize(getPrefix() + translateColorCodes("&c缺少语言文本: " + path));
        }
        return renderToComponent(langFile.getString("prefix", "") + applyPlaceholders(value, placeholders));
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
            values = List.of("&c缺少语言文本: " + path);
        }

        List<String> translated = new ArrayList<>();
        for (String value : values) {
            translated.add(renderToLegacy(value));
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
        config.addDefault("debug", false);
        config.options().copyDefaults(true);

        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("保存 config.yml 失败: " + e.getMessage());
        }
    }

    private void migrateConfigIfNeeded(File configFile) {
        FileConfiguration existingConfig = YamlConfiguration.loadConfiguration(configFile);
        int existingVersion = existingConfig.getInt("config-version", 0);
        if (existingVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }

        File backupFile = new File(plugin.getDataFolder(),
            "config.v" + existingVersion + ".bak-" + System.currentTimeMillis() + ".yml");

        try {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            InputStream defaultConfigStream = plugin.getResource("config.yml");
            if (defaultConfigStream != null) {
                YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8)
                );
                for (String path : existingConfig.getKeys(true)) {
                    boolean migratedFeaturePath = path.startsWith("skin-bridge.") || path.startsWith("blocks-menu.");
                    if (!existingConfig.isConfigurationSection(path)
                        && (migratedConfig.contains(path) || migratedFeaturePath)) {
                        migratedConfig.set(path, existingConfig.get(path));
                    }
                }
                boolean debugEnabled = existingConfig.getBoolean("debug", false)
                    || existingConfig.getBoolean("jei-sync.debug", false)
                    || existingConfig.getBoolean("skin-bridge.debug", false);
                migratedConfig.set("debug", debugEnabled);
                migratedConfig.set("jei-sync.debug", null);
                migratedConfig.set("config-version", CURRENT_CONFIG_VERSION);
                migratedConfig.save(configFile);
            } else {
                existingConfig.set("config-version", CURRENT_CONFIG_VERSION);
                existingConfig.save(configFile);
            }

            plugin.getLogger().info("已将 config.yml 从版本 " + existingVersion
                + " 迁移到 " + CURRENT_CONFIG_VERSION + "，备份文件: " + backupFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("迁移 config.yml 失败: " + e.getMessage());
        }
    }

    private void loadLanguage() {
        currentLanguage = config.getString("language", "zh_CN");

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            plugin.getLogger().warning("创建语言文件夹失败: " + langFolder.getAbsolutePath());
        }

        File langFileObj = new File(langFolder, currentLanguage + ".yml");
        if (!langFileObj.exists()) {
            if (plugin.getResource("lang/" + currentLanguage + ".yml") != null) {
                plugin.saveResource("lang/" + currentLanguage + ".yml", false);
            } else {
                plugin.getLogger().warning("未找到语言文件: " + currentLanguage + ".yml，已回退到 en_US");
                currentLanguage = "en_US";
                plugin.saveResource("lang/en_US.yml", false);
                langFileObj = new File(langFolder, "en_US.yml");
            }
        }

        langFile = YamlConfiguration.loadConfiguration(langFileObj);
        loadDefaultLanguageFallback();
    }

    private void loadDefaultLanguageFallback() {
        InputStream selectedLangStream = plugin.getResource("lang/" + currentLanguage + ".yml");
        if (selectedLangStream == null) {
            return;
        }

        YamlConfiguration selectedDefaults = YamlConfiguration.loadConfiguration(
            new InputStreamReader(selectedLangStream, StandardCharsets.UTF_8)
        );
        if (!"en_US".equalsIgnoreCase(currentLanguage)) {
            InputStream englishLangStream = plugin.getResource("lang/en_US.yml");
            if (englishLangStream != null) {
                YamlConfiguration englishDefaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(englishLangStream, StandardCharsets.UTF_8)
                );
                selectedDefaults.setDefaults(englishDefaults);
            }
        }
        langFile.setDefaults(selectedDefaults);
    }

    private String applyPlaceholders(String value, Map<String, String> placeholders) {
        String result = value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", escapeMiniMessageReplacement(entry.getValue()));
        }
        return result;
    }

    private String escapeMiniMessageReplacement(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("<", "\\<");
    }

    private String translateColorCodes(String text) {
        if (text == null) {
            return "";
        }

        String themedText = text;
        for (Map.Entry<String, String> color : THEME_COLORS.entrySet()) {
            themedText = themedText.replace(color.getKey(), color.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', expandHexColors(themedText));
    }

    private String expandHexColors(String text) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder legacyHex = new StringBuilder("&x");
            for (char digit : hex.toCharArray()) {
                legacyHex.append('&').append(digit);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(legacyHex.toString()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String renderToLegacy(String text) {
        return LegacyComponentSerializer.legacySection().serialize(renderToComponent(text));
    }

    private Component renderToComponent(String text) {
        if (text == null) {
            return Component.empty();
        }
        if (looksLikeMiniMessage(text)) {
            try {
                return MINI_MESSAGE.deserialize(text);
            } catch (RuntimeException ignored) {
                // 配置中 MiniMessage 语法错误时回退到旧颜色码解析，避免消息完全不可用。
            }
        }
        return LegacyComponentSerializer.legacySection().deserialize(translateColorCodes(text));
    }

    private boolean looksLikeMiniMessage(String text) {
        int open = text.indexOf('<');
        return open >= 0 && text.indexOf('>', open) > open;
    }
}
