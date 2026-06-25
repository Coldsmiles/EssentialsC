package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.teleport.TeleportRequestManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TpDenyCommand extends BaseCommand implements TabCompleter {

    public TpDenyCommand() {
        super("essentialsc.command.tpdeny");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        if (args.length > 1) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.usage-tpdeny"));
            return true;
        }

        TeleportRequestManager manager = plugin.getTeleportRequestManager();
        if (manager == null) {
            player.sendMessage(getLang().getPrefixedString("messages.module-disabled"));
            return true;
        }
        if (manager.isIgnoringRequests(player)) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.ignoring-requests"));
            return true;
        }

        Optional<TeleportRequestManager.TeleportRequest> request = manager.findIncoming(
            player,
            args.length == 0 ? null : args[0]
        );
        if (request.isEmpty()) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.no-request"));
            return true;
        }

        TeleportRequestManager.TeleportRequest denied = request.get();
        TeleportRequestManager.TeleportResult result = manager.deny(player, denied);
        if (result.status() == TeleportRequestManager.TeleportResult.Status.EXPIRED) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.expired", manager.placeholders(denied)));
            return true;
        }

        Map<String, String> placeholders = manager.placeholders(denied);
        player.sendMessage(getLang().getPrefixedString("tpa.messages.denied-target", placeholders));
        Player requester = Bukkit.getPlayer(denied.requesterId());
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(getLang().getPrefixedString("tpa.messages.denied-sender", placeholders));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return List.of();
        }

        TeleportRequestManager manager = plugin.getTeleportRequestManager();
        if (manager == null) {
            return List.of();
        }
        return manager.getIncomingRequesterNames(player, args[0]);
    }
}
