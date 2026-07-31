package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.commands.VanishCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public final class VanishListener implements Listener {

    private final EssentialsC plugin;
    private final BukkitTask permissionTask;

    public VanishListener(EssentialsC plugin) {
        this.plugin = plugin;
        VanishCommand.loadState(plugin);
        this.permissionTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkPermissions, 40L, 40L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (VanishCommand.isVanished(player) && !player.hasPermission("essentialsc.command.vanish")) {
            VanishCommand.restoreVisibility(plugin, player, false);
        }
        if (VanishCommand.isVanished(player)) {
            event.joinMessage(null);
            plugin.getServer().getScheduler().runTask(plugin, () -> VanishCommand.applyHiddenState(plugin, player));
        }
        VanishCommand.hideVanishedPlayersFrom(plugin, event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (VanishCommand.isVanished(player)) {
            event.quitMessage(null);
        }
    }

    public void shutdown() {
        permissionTask.cancel();
    }

    private void checkPermissions() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (VanishCommand.isVanished(player) && !player.hasPermission("essentialsc.command.vanish")) {
                VanishCommand.restoreVisibility(plugin, player, true);
            }
        }
    }
}
