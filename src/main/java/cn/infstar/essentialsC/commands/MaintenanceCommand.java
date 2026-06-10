package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.maintenance.MaintenanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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

        switch (args[0].toLowerCase()) {
            case "on", "enable", "enabled" -> {
                maintenanceManager.setEnabled(true);
                sender.sendMessage(getLang().getPrefixedString("maintenance.messages.enabled"));
                return true;
            }
            case "off", "disable", "disabled" -> {
                maintenanceManager.setEnabled(false);
                sender.sendMessage(getLang().getPrefixedString("maintenance.messages.disabled"));
                return true;
            }
            case "reload" -> {
                maintenanceManager.reload();
                sender.sendMessage(getLang().getPrefixedString("maintenance.messages.reloaded"));
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
            java.util.Map.of("status", status)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !sender.hasPermission(getPermission())) {
            return List.of();
        }

        String partial = args[0].toLowerCase();
        List<String> completions = new ArrayList<>();
        for (String option : List.of("on", "off", "status", "reload")) {
            if (option.startsWith(partial)) {
                completions.add(option);
            }
        }
        return completions;
    }
}
