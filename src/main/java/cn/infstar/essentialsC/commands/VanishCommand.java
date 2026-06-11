package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishCommand extends BaseCommand {

    private static final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishCommand() {
        super("essentialsc.command.vanish");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        if (vanishedPlayers.contains(uuid)) {
            vanishedPlayers.remove(uuid);
            showPlayerToAll(plugin, player);
            player.sendMessage(getLang().getPrefixedString("messages.vanish-disabled"));
        } else {
            vanishedPlayers.add(uuid);
            hidePlayerFromAll(plugin, player);
            player.sendMessage(getLang().getPrefixedString("messages.vanish-enabled"));
        }
        return true;
    }

    public static void hideVanishedPlayersFrom(EssentialsC plugin, Player observer) {
        for (Player vanished : plugin.getServer().getOnlinePlayers()) {
            if (!observer.equals(vanished) && isVanished(vanished)) {
                observer.hidePlayer(plugin, vanished);
            }
        }
    }

    public static void removeVanished(Player player) {
        vanishedPlayers.remove(player.getUniqueId());
    }

    public static void clearAll(EssentialsC plugin) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isVanished(player)) {
                showPlayerToAll(plugin, player);
            }
        }
        vanishedPlayers.clear();
    }

    private static void hidePlayerFromAll(EssentialsC plugin, Player player) {
        for (Player online : player.getServer().getOnlinePlayers()) {
            if (online != player) {
                online.hidePlayer(plugin, player);
            }
        }
    }

    private static void showPlayerToAll(EssentialsC plugin, Player player) {
        for (Player online : player.getServer().getOnlinePlayers()) {
            if (online != player) {
                online.showPlayer(plugin, player);
            }
        }
    }

    public static boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }
}
