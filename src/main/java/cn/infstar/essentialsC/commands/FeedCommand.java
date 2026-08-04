package cn.infstar.essentialsC.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class FeedCommand extends BaseCommand {

    public FeedCommand() {
        super("essentialsc.command.feed");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        if (args.length == 0) {
            feedPlayer(player);
            player.sendMessage(getLang().getPrefixedString("messages.feed-self"));
            return true;
        }

        if (!player.hasPermission("essentialsc.command.feed.others")) {
            player.sendMessage(getLang().getPrefixedString("messages.no-permission-others"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(getLang().getPrefixedString("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }

        feedPlayer(target);
        player.sendMessage(getLang().getPrefixedString("messages.feed-other", Map.of("player", target.getName())));
        target.sendMessage(getLang().getPrefixedString("messages.feed-by-other", Map.of("admin", player.getName())));
        return true;
    }

    @Override
    protected boolean executeConsole(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(getLang().getPrefixedString("messages.feed-usage-console"));
            return true;
        }
        if (!sender.hasPermission("essentialsc.command.feed.others")) {
            sender.sendMessage(getLang().getPrefixedString("messages.no-permission-others"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(getLang().getPrefixedString("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }

        feedPlayer(target);
        sender.sendMessage(getLang().getPrefixedString("messages.feed-other", Map.of("player", target.getName())));
        target.sendMessage(getLang().getPrefixedString("messages.feed-by-other",
            Map.of("admin", getLang().getString("messages.console-name"))));
        return true;
    }

    private void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }
}
