package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.teleport.TeleportRequestManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TpAcceptCommand extends BaseCommand implements TabCompleter {

    public TpAcceptCommand() {
        super("essentialsc.command.tpaccept");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        if (args.length > 1) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.usage-tpaccept"));
            return true;
        }

        TeleportRequestManager manager = plugin.getTeleportRequestManager();
        if (manager == null) {
            player.sendMessage(getLang().getPrefixedComponent("messages.module-disabled"));
            return true;
        }
        if (manager.isIgnoringRequests(player)) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.ignoring-requests"));
            return true;
        }

        Optional<TeleportRequestManager.TeleportRequest> request = manager.findIncoming(
            player,
            args.length == 0 ? null : args[0]
        );
        if (request.isEmpty()) {
            player.sendMessage(args.length == 0
                ? getLang().getPrefixedComponent("tpa.messages.no-request")
                : getLang().getPrefixedComponent("tpa.messages.invalid-request", Map.of("requester", args[0])));
            return true;
        }

        TeleportRequestManager.TeleportRequest accepted = request.get();
        Map<String, String> placeholders = manager.placeholders(accepted);
        TeleportRequestManager.TeleportResult result = manager.accept(player, accepted);
        if (result.status() == TeleportRequestManager.TeleportResult.Status.EXPIRED) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.expired", placeholders));
            return true;
        }
        if (result.status() == TeleportRequestManager.TeleportResult.Status.PLAYER_OFFLINE) {
            return true;
        }
        if (result.status() == TeleportRequestManager.TeleportResult.Status.ON_COOLDOWN) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.accept-cooldown",
                Map.of("seconds", String.valueOf(result.cooldownSeconds()))));
            return true;
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
