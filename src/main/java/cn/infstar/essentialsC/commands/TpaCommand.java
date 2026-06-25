package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.teleport.TeleportRequestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            player.sendMessage(getLang().getPrefixedString(type == TeleportRequestManager.TeleportRequest.Type.TPA
                ? "tpa.messages.usage-tpa"
                : "tpa.messages.usage-tpahere"));
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

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline() || VanishCommand.isVanished(target)) {
            player.sendMessage(getLang().getPrefixedString("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.self"));
            return true;
        }

        TeleportRequestManager.CreateRequestResult createdRequest = manager.createRequest(player, target, type);
        if (createdRequest.status() == TeleportRequestManager.CreateRequestStatus.DUPLICATE) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.duplicate-request", Map.of("target", target.getName())));
            return true;
        }
        if (createdRequest.status() == TeleportRequestManager.CreateRequestStatus.ON_COOLDOWN) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.send-cooldown",
                Map.of("seconds", String.valueOf(createdRequest.cooldownSeconds()))));
            return true;
        }

        TeleportRequestManager.TeleportRequest request = createdRequest.request();
        Map<String, String> placeholders = manager.placeholders(request);
        player.sendMessage(getLang().getPrefixedString(type == TeleportRequestManager.TeleportRequest.Type.TPA
            ? "tpa.messages.sent-tpa"
            : "tpa.messages.sent-tpahere", placeholders));
        if (createdRequest.status() == TeleportRequestManager.CreateRequestStatus.IGNORED) {
            return true;
        }
        target.sendMessage(getLang().getPrefixedString(type == TeleportRequestManager.TeleportRequest.Type.TPA
            ? "tpa.messages.received-tpa"
            : "tpa.messages.received-tpahere", placeholders));
        manager.playRequestReceivedSound(target);
        sendResponseHint(target, request, placeholders);
        return true;
    }

    private void sendResponseHint(Player target, TeleportRequestManager.TeleportRequest request, Map<String, String> placeholders) {
        String requesterName = request.requesterName();
        String acceptCommand = "/tpaccept " + requesterName;
        String denyCommand = "/tpdeny " + requesterName;

        Component hint = LegacyComponentSerializer.legacySection()
            .deserialize(getLang().getPrefixedString("tpa.messages.response-hint", placeholders));
        Component accept = LegacyComponentSerializer.legacySection()
            .deserialize(getLang().getString("tpa.messages.accept-button"))
            .clickEvent(ClickEvent.runCommand(acceptCommand))
            .hoverEvent(HoverEvent.showText(Component.text(acceptCommand, NamedTextColor.GREEN)));
        Component deny = LegacyComponentSerializer.legacySection()
            .deserialize(getLang().getString("tpa.messages.deny-button"))
            .clickEvent(ClickEvent.runCommand(denyCommand))
            .hoverEvent(HoverEvent.showText(Component.text(denyCommand, NamedTextColor.RED)));

        target.sendMessage(hint.append(Component.space()).append(accept).append(Component.space()).append(deny));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return List.of();
        }

        String partial = args[0].toLowerCase();
        List<String> completions = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.getUniqueId().equals(player.getUniqueId())
                && !VanishCommand.isVanished(onlinePlayer)
                && onlinePlayer.getName().toLowerCase().startsWith(partial)) {
                completions.add(onlinePlayer.getName());
            }
        }
        return completions;
    }
}
