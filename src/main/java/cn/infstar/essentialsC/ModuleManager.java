package cn.infstar.essentialsC;

import cn.infstar.essentialsC.util.AtomicYamlWriter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleManager {

    private static final int CURRENT_CONFIG_VERSION = 1;

    public static final String BLOCKS = "blocks";
    public static final String ADMIN_MODE = "admin-mode";
    public static final String TPSBAR = "tpsbar";
    public static final String MOB_DROPS = "mob-drops";
    public static final String MAINTENANCE = "maintenance";
    public static final String SKIN_BRIDGE = "skin-bridge";

    private static final Map<String, Boolean> DEFAULT_MODULES = new LinkedHashMap<>();

    static {
        DEFAULT_MODULES.put(BLOCKS, true);
        DEFAULT_MODULES.put(ADMIN_MODE, true);
        DEFAULT_MODULES.put(TPSBAR, true);
        DEFAULT_MODULES.put(MOB_DROPS, false);
        DEFAULT_MODULES.put(MAINTENANCE, true);
        DEFAULT_MODULES.put(SKIN_BRIDGE, false);
    }

    private final JavaPlugin plugin;
    private final File modulesFile;
    private FileConfiguration modulesConfig;

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.modulesFile = new File(plugin.getDataFolder(), "modules.yml");
        reload();
    }

    public void reload() {
        if (!modulesFile.exists()) {
            plugin.saveResource("modules.yml", false);
        }

        YamlConfiguration loaded = new YamlConfiguration();
        loaded.options().parseComments(true);
        try {
            loaded.load(modulesFile);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("加载 modules.yml 失败: " + exception.getMessage());
            throw new IllegalStateException("无法加载 modules.yml，请修复配置格式后重试。", exception);
        }
        modulesConfig = loaded;
        boolean removedJeiSync = modulesConfig.contains("modules.jei-sync", true);
        if (removedJeiSync) {
            modulesConfig.set("modules.jei-sync", null);
        }
        modulesConfig.addDefault("config-version", CURRENT_CONFIG_VERSION);
        for (Map.Entry<String, Boolean> module : DEFAULT_MODULES.entrySet()) {
            modulesConfig.addDefault(path(module.getKey()), module.getValue());
        }
        modulesConfig.options().copyDefaults(true);
        if (removedJeiSync) {
            try {
                AtomicYamlWriter.save(modulesConfig, modulesFile);
                plugin.getLogger().info("已从 modules.yml 移除停用的 JEI 配方同步配置。");
            } catch (IOException exception) {
                plugin.getLogger().warning("清理 modules.yml 中的 JEI 配置失败: " + exception.getMessage());
            }
        }
    }

    public boolean isEnabled(String moduleKey) {
        if (moduleKey == null || moduleKey.isBlank()) {
            return true;
        }
        return modulesConfig.getBoolean(path(moduleKey), true);
    }

    private String path(String moduleKey) {
        return "modules." + moduleKey + ".enabled";
    }

}
