package cn.infstar.essentialsC.maintenance;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.ModuleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.net.InetAddress;
import java.util.Map;

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

        if (maintenanceManager.canJoin(event.getPlayer())) {
            return;
        }

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, maintenanceManager.getKickMessage());
        notifyBlockedLogin(event, maintenanceManager);
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

    private void notifyBlockedLogin(PlayerLoginEvent event, MaintenanceManager maintenanceManager) {
        if (!maintenanceManager.isNotifyEnabled()) {
            return;
        }

        String address = EssentialsC.getLangManager().getString("maintenance.status.address-hidden");
        if (maintenanceManager.shouldIncludeAddressInNotification()) {
            InetAddress inetAddress = event.getAddress();
            if (inetAddress != null) {
                address = inetAddress.getHostAddress();
            }
        }

        String message = EssentialsC.getLangManager().getPrefixedString("maintenance.messages.login-blocked",
            Map.of(
                "player", event.getPlayer().getName(),
                "uuid", event.getPlayer().getUniqueId().toString(),
                "address", address
            ));

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission(maintenanceManager.getNotifyPermission())) {
                player.sendMessage(message);
            }
        }
    }
}
