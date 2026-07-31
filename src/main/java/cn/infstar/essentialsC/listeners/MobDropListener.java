package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobDropListener implements Listener {

    private final EssentialsC plugin;
    private boolean endermanDropsAllowed;

    public MobDropListener(EssentialsC plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        config.addDefault("mob-drops.enderman.allow-drops", true);
        config.options().copyDefaults(true);

        this.endermanDropsAllowed = config.getBoolean("mob-drops.enderman.allow-drops", true);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDERMAN) {
            return;
        }

        if (!plugin.getConfig().getBoolean("mob-drops.enderman.allow-drops", true)) {
            event.getDrops().clear();
        }
    }

    public void reload() {
        loadConfig();
        plugin.getLogger().info("生物掉落配置已重载（末影人掉落: " + (endermanDropsAllowed ? "允许" : "禁止") + "）");
    }
}
