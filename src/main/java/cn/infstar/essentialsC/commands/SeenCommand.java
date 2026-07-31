package cn.infstar.essentialsC.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class SeenCommand extends BaseCommand {

    public SeenCommand() {
        super("essentialsc.command.seen");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(getLang().getPrefixedString("messages.seen-usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(getLang().getPrefixedString("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        player.sendMessage(getLang().getPrefixedComponent("messages.seen-header",
            Map.of("player", String.valueOf(target.getName()))));

        if (target.isOnline()) {
            player.sendMessage(getLang().getComponent("messages.seen-status-online"));
            Player onlinePlayer = target.getPlayer();
            if (onlinePlayer != null) {
                player.sendMessage(getLang().getComponent("messages.seen-world",
                    Map.of("world", onlinePlayer.getWorld().getName())));
            }
        } else {
            player.sendMessage(getLang().getComponent("messages.seen-status-offline"));
            long lastSeen = target.getLastSeen();
            if (lastSeen > 0) {
                player.sendMessage(getLang().getComponent("messages.seen-last-online",
                    Map.of("time", format.format(new Date(lastSeen)))));
            }
        }

        player.sendMessage(getLang().getComponent("messages.seen-first-joined",
            Map.of("time", format.format(new Date(target.getFirstPlayed())))));
        return true;
    }
}
