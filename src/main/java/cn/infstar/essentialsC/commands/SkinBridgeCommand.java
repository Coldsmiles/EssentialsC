package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.skinbridge.SkinBridgeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkinBridgeCommand extends BaseCommand implements TabCompleter {

    private static final String STATUS_PERMISSION = "essentialsc.command.skin.status";
    private static final String REFRESH_PERMISSION = "essentialsc.command.skin.refresh";
    private static final String OTHERS_PERMISSION = "essentialsc.command.skin.others";

    public SkinBridgeCommand() {
        super("essentialsc.command.skin");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        return executeCommand(player, args, false);
    }

    @Override
    protected boolean executeConsole(CommandSender sender, String[] args) {
        return executeCommand(sender, args, true);
    }

    private boolean executeCommand(CommandSender sender, String[] args, boolean console) {
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.usage"));
            return true;
        }

        SkinBridgeManager manager = plugin.getSkinBridgeManager();
        if (manager == null) {
            sender.sendMessage(getLang().getPrefixedString("messages.module-disabled"));
            return true;
        }

        Player target = console ? null : (Player) sender;
        if (args.length == 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(getLang().getPrefixedString("messages.player-not-found", Map.of("player", args[1])));
                return true;
            }
        }
        if (target == null) {
            sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.usage"));
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        boolean targetsOther = sender instanceof Player playerSender
            && !playerSender.getUniqueId().equals(target.getUniqueId());
        if (action.equals("status")) {
            if (!hasPermission(sender, STATUS_PERMISSION)) {
                sendNoPermission(sender, STATUS_PERMISSION);
                return true;
            }
            if (targetsOther && !hasPermission(sender, OTHERS_PERMISSION)) {
                sendNoPermission(sender, OTHERS_PERMISSION);
                return true;
            }
            sendStatus(sender, target, manager);
            return true;
        }
        if (action.equals("refresh")) {
            if (!hasPermission(sender, REFRESH_PERMISSION)) {
                sendNoPermission(sender, REFRESH_PERMISSION);
                return true;
            }
            if (targetsOther && !hasPermission(sender, OTHERS_PERMISSION)) {
                sendNoPermission(sender, OTHERS_PERMISSION);
                return true;
            }
            sendRefreshResult(sender, target, manager.queueSync(target, true));
            return true;
        }

        sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.usage"));
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return !(sender instanceof Player) || sender.hasPermission(permission);
    }

    private void sendNoPermission(CommandSender sender, String permission) {
        sender.sendMessage(getLang().getPrefixedString("messages.no-permission", Map.of("permission", permission)));
    }

    private void sendStatus(CommandSender sender, Player target, SkinBridgeManager manager) {
        if (!manager.isSkinGatewayAvailable()) {
            sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.dependency-missing"));
            return;
        }
        if (manager.getProviderCount() == 0) {
            sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.no-providers"));
            return;
        }

        SkinBridgeManager.Status status = manager.getStatus(target);
        Map<String, String> placeholders = Map.of("player", target.getName(), "provider", String.valueOf(status.providerId()));
        switch (status.state()) {
            case EXTERNAL -> sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.status-external", placeholders));
            case EXCLUDED -> sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.status-excluded", placeholders));
            case NOT_EXTERNAL -> sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.status-not-external", placeholders));
            case PENDING -> sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.status-pending", placeholders));
            case UNKNOWN -> sender.sendMessage(getLang().getPrefixedString("skin-bridge.messages.status-unknown", placeholders));
        }
    }

    private void sendRefreshResult(CommandSender sender, Player target, SkinBridgeManager.SyncResult result) {
        Map<String, String> placeholders = Map.of("player", target.getName());
        String messagePath = switch (result) {
            case QUEUED -> "skin-bridge.messages.refresh-queued";
            case CACHED -> "skin-bridge.messages.refresh-cached";
            case ALREADY_RUNNING -> "skin-bridge.messages.refresh-running";
            case EXCLUDED -> "skin-bridge.messages.refresh-excluded";
            case QUEUE_FULL -> "skin-bridge.messages.queue-full";
            case DEPENDENCY_MISSING -> "skin-bridge.messages.dependency-missing";
            case NO_PROVIDERS -> "skin-bridge.messages.no-providers";
            case REFRESH_COOLDOWN -> "skin-bridge.messages.refresh-cooldown";
        };
        if (result == SkinBridgeManager.SyncResult.REFRESH_COOLDOWN) {
            placeholders = Map.of(
                "player", target.getName(),
                "seconds", String.valueOf(plugin.getSkinBridgeManager().getRemainingForceRefreshCooldownSeconds(target))
            );
        }
        sender.sendMessage(getLang().getPrefixedString(messagePath, placeholders));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> actions = new ArrayList<>();
            if (hasPermission(sender, STATUS_PERMISSION)) {
                actions.add("status");
            }
            if (hasPermission(sender, REFRESH_PERMISSION)) {
                actions.add("refresh");
            }
            return actions.stream()
                .filter(option -> option.startsWith(partial))
                .toList();
        }
        if (args.length == 2 && hasPermission(sender, OTHERS_PERMISSION)
            && ((args[0].equalsIgnoreCase("status") && hasPermission(sender, STATUS_PERMISSION))
            || (args[0].equalsIgnoreCase("refresh") && hasPermission(sender, REFRESH_PERMISSION)))) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            List<String> players = new ArrayList<>();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase(Locale.ROOT).startsWith(partial)) {
                    players.add(onlinePlayer.getName());
                }
            }
            return players;
        }
        return List.of();
    }
}
