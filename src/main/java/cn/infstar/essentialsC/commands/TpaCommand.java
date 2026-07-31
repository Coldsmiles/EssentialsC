package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.teleport.TeleportRequestManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TpaCommand extends BaseCommand implements TabCompleter {

    public TpaCommand() {
        super("essentialsc.command.tpa");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        return sendRequest(player, args, TeleportRequestManager.TeleportRequest.Type.TPA);
    }

    protected boolean sendRequest(Player player, String[] args, TeleportRequestManager.TeleportRequest.Type type) {
        if (args.length != 1) {
            player.sendMessage(getLang().getPrefixedComponent(type == TeleportRequestManager.TeleportRequest.Type.TPA
                ? "tpa.messages.usage-tpa"
                : "tpa.messages.usage-tpahere"));
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

        if (args[0].equalsIgnoreCase(player.getName())) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.self"));
            return true;
        }

        int remainingCooldown = manager.getRemainingSendCooldownSeconds(player);
        if (remainingCooldown > 0) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.send-cooldown",
                Map.of("seconds", String.valueOf(remainingCooldown))));
            return true;
        }

        Player target = manager.findOnlinePlayer(args[0], onlinePlayer ->
            !onlinePlayer.getUniqueId().equals(player.getUniqueId()) && !manager.isVanished(onlinePlayer)
        ).orElse(null);
        if (target == null || !target.isOnline()) {
            player.sendMessage(getLang().getPrefixedComponent("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }

        TeleportRequestManager.CreateRequestResult createdRequest = manager.createRequest(player, target, type);
        if (createdRequest.status() == TeleportRequestManager.CreateRequestStatus.ON_COOLDOWN) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.send-cooldown",
                Map.of("seconds", String.valueOf(createdRequest.cooldownSeconds()))));
            return true;
        }

        TeleportRequestManager.TeleportRequest request = createdRequest.request();
        Map<String, String> placeholders = manager.placeholders(request);
        player.sendMessage(getLang().getPrefixedComponent(type == TeleportRequestManager.TeleportRequest.Type.TPA
            ? "tpa.messages.sent-tpa"
            : "tpa.messages.sent-tpahere", placeholders));
        if (createdRequest.status() == TeleportRequestManager.CreateRequestStatus.IGNORED
            || createdRequest.status() == TeleportRequestManager.CreateRequestStatus.DUPLICATE) {
            return true;
        }
        target.sendMessage(getLang().getPrefixedComponent(type == TeleportRequestManager.TeleportRequest.Type.TPA
            ? "tpa.messages.received-tpa"
            : "tpa.messages.received-tpahere", placeholders));
        manager.playRequestReceivedSound(target);
        manager.sendResponseHint(target, placeholders);
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
        String partial = args[0].toLowerCase();
        List<String> completions = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.getUniqueId().equals(player.getUniqueId())
                && !manager.isVanished(onlinePlayer)
                && onlinePlayer.getName().toLowerCase().startsWith(partial)) {
                completions.add(onlinePlayer.getName());
            }
        }
        return completions;
    }
}
