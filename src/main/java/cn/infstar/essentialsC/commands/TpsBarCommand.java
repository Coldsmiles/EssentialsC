package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.tpsbar.TpsBarService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TpsBarCommand extends BaseCommand implements TabCompleter {

    public TpsBarCommand() {
        super("essentialsc.command.tpsbar");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        TpsBarService tpsBarService = plugin.getTpsBarManager();
        if (tpsBarService == null) {
            player.sendMessage(getLang().getPrefixedString("messages.player-only"));
            return true;
        }

        if (args.length == 0) {
            boolean enabled = tpsBarService.toggle(player);
            tpsBarService.sendToggleMessage(player, player, enabled);
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(tpsBarService.getUsageMessage());
            return true;
        }

        if (!player.hasPermission("essentialsc.command.tpsbar.others")) {
            player.sendMessage(getLang().getPrefixedString("messages.no-permission",
                Map.of("permission", "essentialsc.command.tpsbar.others")));
            return true;
        }

        Collection<Player> targets = tpsBarService.resolveTargets(player, args[0]);
        if (targets.isEmpty()) {
            Player exactPlayer = Bukkit.getPlayerExact(args[0]);
            if (exactPlayer == null) {
                player.sendMessage(tpsBarService.getPlayerNotFoundMessage(args[0]));
            } else {
                player.sendMessage(tpsBarService.getNoTargetsMessage());
            }
            return true;
        }

        for (Player target : targets) {
            boolean enabled = tpsBarService.toggle(target);
            tpsBarService.sendToggleMessage(player, target, enabled);
        }
        return true;
    }

    @Override
    protected boolean executeConsole(CommandSender sender, String[] args) {
        sender.sendMessage(getLang().getPrefixedString("messages.player-only"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return List.of();
        }

        if (!player.hasPermission("essentialsc.command.tpsbar.others")) {
            return List.of();
        }

        String partial = args[0].toLowerCase();
        List<String> completions = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().toLowerCase().startsWith(partial)) {
                completions.add(onlinePlayer.getName());
            }
        }
        return completions;
    }
}
