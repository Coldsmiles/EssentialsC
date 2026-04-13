package cn.infstar.essentialsC;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LangManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration langFile;
    private String currentLanguage;
    
    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadLanguage();
    }
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 设置默认值
        config.addDefault("language", "zh_CN");
        config.addDefault("settings.enable-feedback", true);
        config.addDefault("settings.message-prefix", "&6[EssentialsC] &r");
        config.options().copyDefaults(true);
        
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("无法保存配置文件: " + e.getMessage());
        }
    }
    
    private void loadLanguage() {
        currentLanguage = config.getString("language", "zh_CN");
        
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        
        File langFileObj = new File(langFolder, currentLanguage + ".yml");
        
        // 如果语言文件不存在，从资源中复制
        if (!langFileObj.exists()) {
            InputStream inputStream = plugin.getResource("lang/" + currentLanguage + ".yml");
            if (inputStream != null) {
                plugin.saveResource("lang/" + currentLanguage + ".yml", false);
            } else {
                plugin.getLogger().warning("未找到语言文件: " + currentLanguage + ".yml，使用默认语言 en_US");
                currentLanguage = "en_US";
                plugin.saveResource("lang/en_US.yml", false);
                langFileObj = new File(langFolder, "en_US.yml");
            }
        }
        
        langFile = YamlConfiguration.loadConfiguration(langFileObj);
        
        // 尝试加载默认语言作为后备
        if (!currentLanguage.equals("en_US")) {
            InputStream defaultLangStream = plugin.getResource("lang/en_US.yml");
            if (defaultLangStream != null) {
                YamlConfiguration defaultLang = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultLangStream, StandardCharsets.UTF_8)
                );
                langFile.setDefaults(defaultLang);
            }
        }
    }
    
    /**
     * 获取插件前缀
     */
    public String getPrefix() {
        return translateColorCodes(langFile.getString("prefix", "&6[EssentialsC] &r"));
    }
    
    /**
     * 获取翻译文本
     */
    public String getString(String path) {
        String value = langFile.getString(path);
        if (value == null) {
            return "&cMissing translation: " + path;
        }
        return translateColorCodes(value);
    }
    
    /**
     * 获取翻译文本并替换占位符
     */
    public String getString(String path, Map<String, String> placeholders) {
        String value = getString(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }
    
    /**
     * 获取字符串列表（用于 Lore 等多行文本）
     */
    public java.util.List<String> getStringList(String path) {
        java.util.List<String> values = langFile.getStringList(path);
        if (values.isEmpty()) {
            // 如果找不到，返回包含错误信息的列表
            return java.util.Arrays.asList("&cMissing translation: " + path);
        }
        // 翻译颜色代码
        java.util.List<String> translated = new java.util.ArrayList<>();
        for (String value : values) {
            translated.add(translateColorCodes(value));
        }
        return translated;
    }
    
    /**
     * 重新加载配置和语言
     */
    public void reload() {
        loadConfig();
        loadLanguage();
    }
    
    /**
     * 获取当前语言
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * 翻译颜色代码
     */
    private String translateColorCodes(String text) {
        return text.replace("&", "§");
    }
}
