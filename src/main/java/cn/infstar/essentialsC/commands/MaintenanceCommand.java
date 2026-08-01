package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.maintenance.MaintenanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MaintenanceCommand extends BaseCommand implements TabCompleter {

    public MaintenanceCommand() {
        super("essentialsc.command.maintenance");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        return executeCommand(player, args);
    }

    @Override
    protected boolean executeConsole(CommandSender sender, String[] args) {
        return executeCommand(sender, args);
    }

    private boolean executeCommand(CommandSender sender, String[] args) {
        MaintenanceManager maintenanceManager = plugin.getMaintenanceManager();
        if (maintenanceManager == null) {
            sender.sendMessage(getLang().getPrefixedString("messages.module-disabled"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sendStatus(sender, maintenanceManager);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on", "enable", "enabled" -> {
                if (maintenanceManager.setEnabled(true) == MaintenanceManager.OperationResult.SAVE_FAILED) {
                    sendSaveFailed(sender);
                    return true;
                }
                sender.sendMessage(getLang().getPrefixedString("maintenance.messages.enabled"));
                return true;
            }
            case "off", "disable", "disabled" -> {
                if (maintenanceManager.setEnabled(false) == MaintenanceManager.OperationResult.SAVE_FAILED) {
                    sendSaveFailed(sender);
                    return true;
                }
                sender.sendMessage(getLang().getPrefixedString("maintenance.messages.disabled"));
                return true;
            }
            case "reload" -> {
                maintenanceManager.reload();
                sender.sendMessage(getLang().getPrefixedString("maintenance.messages.reloaded"));
                return true;
            }
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage(getLang().getPrefixedString("maintenance.messages.add-usage"));
                    return true;
                }

                String target = args[1];
                MaintenanceManager.OperationResult result = maintenanceManager.addWhitelistEntry(target);
                if (result == MaintenanceManager.OperationResult.SAVE_FAILED) {
                    sendSaveFailed(sender);
                } else if (result == MaintenanceManager.OperationResult.SUCCESS) {
                    sender.sendMessage(getLang().getPrefixedString("maintenance.messages.whitelist-added",
                        Map.of("player", target)));
                } else {
                    sender.sendMessage(getLang().getPrefixedString("maintenance.messages.whitelist-exists",
                        Map.of("player", target)));
                }
                return true;
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage(getLang().getPrefixedString("maintenance.messages.remove-usage"));
                    return true;
                }

                String target = args[1];
                MaintenanceManager.OperationResult result = maintenanceManager.removeWhitelistEntry(target);
                if (result == MaintenanceManager.OperationResult.SAVE_FAILED) {
                    sendSaveFailed(sender);
                } else if (result == MaintenanceManager.OperationResult.SUCCESS) {
                    sender.sendMessage(getLang().getPrefixedString("maintenance.messages.whitelist-removed",
                        Map.of("player", target)));
                } else {
                    sender.sendMessage(getLang().getPrefixedString("maintenance.messages.whitelist-missing",
                        Map.of("player", target)));
                }
                return true;
            }
            case "list" -> {
                sendWhitelist(sender, maintenanceManager);
                return true;
            }
            default -> {
                sender.sendMessage(getLang().getPrefixedString("maintenance.messages.usage"));
                return true;
            }
        }
    }

    private void sendStatus(CommandSender sender, MaintenanceManager maintenanceManager) {
        String status = getLang().getString(maintenanceManager.isEnabled()
            ? "maintenance.status.enabled"
            : "maintenance.status.disabled");
        sender.sendMessage(getLang().getPrefixedString("maintenance.messages.status",
            Map.of(
                "status", status,
                "whitelist_count", String.valueOf(maintenanceManager.getWhitelistCount())
            )));
    }

    private void sendWhitelist(CommandSender sender, MaintenanceManager maintenanceManager) {
        List<String> entries = maintenanceManager.getWhitelistEntries();
        if (entries.isEmpty()) {
            sender.sendMessage(getLang().getPrefixedString("maintenance.messages.whitelist-empty"));
            return;
        }

        sender.sendMessage(getLang().getPrefixedString("maintenance.messages.whitelist-list",
            Map.of(
                "count", String.valueOf(entries.size()),
                "entries", String.join(", ", entries)
            )));
    }

    private void sendSaveFailed(CommandSender sender) {
        sender.sendMessage(getLang().getPrefixedString("maintenance.messages.save-failed"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(getPermission())) {
            return List.of();
        }

        if (args.length == 1) {
            return complete(args[0], List.of("on", "off", "status", "reload", "add", "remove", "list"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return complete(args[1], plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            MaintenanceManager maintenanceManager = plugin.getMaintenanceManager();
            if (maintenanceManager == null) {
                return List.of();
            }
            return complete(args[1], maintenanceManager.getWhitelistEntries());
        }

        return List.of();
    }

    private List<String> complete(String input, List<String> options) {
        String partial = input.toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(partial)) {
                completions.add(option);
            }
        }
        return completions;
    }
}
