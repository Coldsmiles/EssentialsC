package cn.infstar.essentialsC.maintenance;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.ModuleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

public final class MaintenanceListener implements Listener {

    private final EssentialsC plugin;
    private final MaintenanceManager maintenanceManager;
    private final MaintenancePermissionResolver permissionResolver;

    public MaintenanceListener(EssentialsC plugin, MaintenanceManager maintenanceManager) {
        this.plugin = plugin;
        this.maintenanceManager = maintenanceManager;
        this.permissionResolver = MaintenancePermissionResolverFactory.create(plugin);
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        if (!plugin.getModuleManager().isEnabled(ModuleManager.MAINTENANCE)
            || !maintenanceManager.isEnabled()
            || !maintenanceManager.isMotdEnabled()) {
            return;
        }

        event.motd(maintenanceManager.getMotd());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        MaintenanceManager.AccessSnapshot snapshot = maintenanceManager.getAccessSnapshot();
        if (!snapshot.enabled()
            || snapshot.isWhitelisted(event.getUniqueId(), event.getName())
            || maintenanceManager.isOperator(event.getUniqueId())) {
            return;
        }

        if (permissionResolver == null) {
            return;
        }

        MaintenancePermissionResolver.PermissionResult permissionResult = permissionResolver
            .checkPermission(event.getUniqueId(), snapshot.bypassPermission());
        if (permissionResult != MaintenancePermissionResolver.PermissionResult.DENIED) {
            return;
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, snapshot.kickMessage());
        notifyBlockedLogin(event.getName(), event.getUniqueId().toString(), event.getAddress(), snapshot);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getModuleManager().isEnabled(ModuleManager.MAINTENANCE)) {
            return;
        }

        Player player = event.getPlayer();
        if (maintenanceManager.isEnabled() && !maintenanceManager.canJoin(player)) {
            MaintenanceManager.AccessSnapshot snapshot = maintenanceManager.getAccessSnapshot();
            player.kick(snapshot.kickMessage());
            InetSocketAddress socketAddress = player.getAddress();
            notifyBlockedLogin(player.getName(), player.getUniqueId().toString(),
                socketAddress == null ? null : socketAddress.getAddress(), snapshot);
            return;
        }

        maintenanceManager.addBossBarPlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        maintenanceManager.removeBossBarPlayer(event.getPlayer());
    }

    private void notifyBlockedLogin(String playerName, String uniqueId, InetAddress inetAddress,
                                    MaintenanceManager.AccessSnapshot snapshot) {
        if (!snapshot.notifyEnabled()) {
            return;
        }

        String visibleAddress = snapshot.includeAddress() && inetAddress != null ? inetAddress.getHostAddress() : null;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String address = visibleAddress == null
                ? EssentialsC.getLangManager().getString("maintenance.status.address-hidden")
                : visibleAddress;
            var message = EssentialsC.getLangManager().getPrefixedComponent("maintenance.messages.login-blocked",
                Map.of("player", playerName, "uuid", uniqueId, "address", address));

            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.hasPermission(snapshot.notifyPermission())) {
                    onlinePlayer.sendMessage(message);
                }
            }
        });
    }
}
