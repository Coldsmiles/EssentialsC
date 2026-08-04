package cn.infstar.essentialsC.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
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
        return executeCommand(player, player, args);
    }

    @Override
    protected boolean executeConsole(CommandSender sender, String[] args) {
        return executeCommand(sender, null, args);
    }

    private boolean executeCommand(CommandSender sender, Player viewer, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getLang().getPrefixedString(viewer == null
                ? "messages.seen-usage-console"
                : "messages.seen-usage"));
            return true;
        }

        Player onlineTarget = Bukkit.getPlayerExact(args[0]);
        if (onlineTarget != null && VanishCommand.isVanished(onlineTarget)
            && viewer != null && !viewer.hasPermission(VanishCommand.SEE_PERMISSION)) {
            sender.sendMessage(getLang().getPrefixedString("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(getLang().getPrefixedString("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sender.sendMessage(getLang().getPrefixedComponent("messages.seen-header",
            Map.of("player", String.valueOf(target.getName()))));

        if (target.isOnline()) {
            sender.sendMessage(getLang().getComponent("messages.seen-status-online"));
            Player onlinePlayer = target.getPlayer();
            if (onlinePlayer != null) {
                sender.sendMessage(getLang().getComponent("messages.seen-world",
                    Map.of("world", onlinePlayer.getWorld().getName())));
            }
        } else {
            sender.sendMessage(getLang().getComponent("messages.seen-status-offline"));
            long lastSeen = target.getLastSeen();
            if (lastSeen > 0) {
                sender.sendMessage(getLang().getComponent("messages.seen-last-online",
                    Map.of("time", format.format(new Date(lastSeen)))));
            }
        }

        sender.sendMessage(getLang().getComponent("messages.seen-first-joined",
            Map.of("time", format.format(new Date(target.getFirstPlayed())))));
        return true;
    }
}
