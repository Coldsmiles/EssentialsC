package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.commands.VanishCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VanishListener implements Listener {

    private final EssentialsC plugin;
    private final BukkitTask permissionTask;
    private final Map<UUID, Boolean> observerSeePermissions = new HashMap<>();

    public VanishListener(EssentialsC plugin) {
        this.plugin = plugin;
        VanishCommand.loadState(plugin);
        this.permissionTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkPermissions, 40L, 40L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        observerSeePermissions.put(player.getUniqueId(), player.hasPermission(VanishCommand.SEE_PERMISSION));
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
        observerSeePermissions.remove(event.getPlayer().getUniqueId());
        Player player = event.getPlayer();
        if (VanishCommand.isVanished(player)) {
            event.quitMessage(null);
        }
    }

    public void shutdown() {
        permissionTask.cancel();
    }

    private void checkPermissions() {
        Set<UUID> onlinePlayers = new HashSet<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            onlinePlayers.add(player.getUniqueId());
            if (VanishCommand.isVanished(player) && !player.hasPermission("essentialsc.command.vanish")) {
                VanishCommand.restoreVisibility(plugin, player, true);
            }
        }
        observerSeePermissions.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));

        for (Player observer : plugin.getServer().getOnlinePlayers()) {
            boolean canSeeVanished = observer.hasPermission(VanishCommand.SEE_PERMISSION);
            Boolean previous = observerSeePermissions.put(observer.getUniqueId(), canSeeVanished);
            if (previous == null || previous != canSeeVanished) {
                refreshObserverVisibility(observer, canSeeVanished);
            }
        }
    }

    private void refreshObserverVisibility(Player observer, boolean canSeeVanished) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (observer.equals(player) || !VanishCommand.isVanished(player)) {
                continue;
            }
            if (canSeeVanished) {
                observer.showPlayer(plugin, player);
            } else {
                observer.hidePlayer(plugin, player);
            }
        }
    }
}
