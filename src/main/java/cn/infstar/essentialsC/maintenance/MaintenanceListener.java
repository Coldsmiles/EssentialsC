package cn.infstar.essentialsC.maintenance;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.ModuleManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

public final class MaintenanceListener implements Listener {

    private final EssentialsC plugin;

    public MaintenanceListener(EssentialsC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        MaintenanceManager maintenanceManager = plugin.getMaintenanceManager();
        if (!plugin.getModuleManager().isEnabled(ModuleManager.MAINTENANCE)
            || maintenanceManager == null
            || !maintenanceManager.isEnabled()
            || !maintenanceManager.isMotdEnabled()) {
            return;
        }

        event.setMotd(maintenanceManager.getMotd());
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        MaintenanceManager maintenanceManager = plugin.getMaintenanceManager();
        if (!plugin.getModuleManager().isEnabled(ModuleManager.MAINTENANCE)
            || maintenanceManager == null
            || !maintenanceManager.isEnabled()) {
            return;
        }

        if (event.getPlayer().hasPermission(maintenanceManager.getBypassPermission())) {
            return;
        }

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, maintenanceManager.getKickMessage());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        MaintenanceManager maintenanceManager = plugin.getMaintenanceManager();
        if (!plugin.getModuleManager().isEnabled(ModuleManager.MAINTENANCE) || maintenanceManager == null) {
            return;
        }

        maintenanceManager.addBossBarPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        MaintenanceManager maintenanceManager = plugin.getMaintenanceManager();
        if (maintenanceManager != null) {
            maintenanceManager.removeBossBarPlayer(event.getPlayer());
        }
    }
}
