package cn.infstar.essentialsC;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleManager {

    private static final int CURRENT_CONFIG_VERSION = 1;

    public static final String BLOCKS = "blocks";
    public static final String PLAYER = "player";
    public static final String ADMIN_MODE = "admin-mode";
    public static final String TPSBAR = "tpsbar";
    public static final String JEI_SYNC = "jei-sync";
    public static final String MOB_DROPS = "mob-drops";
    public static final String MAINTENANCE = "maintenance";
    public static final String SKIN_BRIDGE = "skin-bridge";

    private static final Map<String, Boolean> DEFAULT_MODULES = new LinkedHashMap<>();

    static {
        DEFAULT_MODULES.put(BLOCKS, true);
        DEFAULT_MODULES.put(PLAYER, true);
        DEFAULT_MODULES.put(ADMIN_MODE, true);
        DEFAULT_MODULES.put(TPSBAR, true);
        DEFAULT_MODULES.put(JEI_SYNC, true);
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

        modulesConfig = YamlConfiguration.loadConfiguration(modulesFile);
        modulesConfig.addDefault("config-version", CURRENT_CONFIG_VERSION);
        for (Map.Entry<String, Boolean> module : DEFAULT_MODULES.entrySet()) {
            modulesConfig.addDefault(path(module.getKey()), module.getValue());
        }
        modulesConfig.options().copyDefaults(true);
        save();
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

    private void save() {
        try {
            modulesConfig.save(modulesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存 modules.yml 失败: " + e.getMessage());
        }
    }
}
