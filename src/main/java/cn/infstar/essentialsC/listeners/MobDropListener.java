package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * 生物掉落物控制监听器
 * 当前仅支持末影人
 */
public class MobDropListener implements Listener {
    
    private final EssentialsC plugin;
    private boolean endermanDropEnabled;
    
    public MobDropListener(EssentialsC plugin) {
        this.plugin = plugin;
        loadConfig();
        
        // 注册监听器
    }
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        config.addDefault("mob-drops.enderman.enabled", true);
        config.options().copyDefaults(true);
        
        try {
            config.save(plugin.getDataFolder().toPath().resolve("config.yml").toFile());
        } catch (Exception e) {
            plugin.getLogger().warning("无法保存配置文件: " + e.getMessage());
        }
        
        this.endermanDropEnabled = config.getBoolean("mob-drops.enderman.enabled", true);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDERMAN) {
            return;
        }
        
        boolean enabled = plugin.getConfig().getBoolean("mob-drops.enderman.enabled", true);
        
        if (!enabled) {
            event.getDrops().clear();
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        loadConfig();
        plugin.getLogger().info("生物掉落物配置已重载（末影人: " + (endermanDropEnabled ? "开启" : "关闭") + "）");
    }
}
