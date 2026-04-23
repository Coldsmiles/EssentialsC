package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.LangManager;
import cn.infstar.essentialsC.tpsbar.TpsBarService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelpCommand extends BaseCommand implements TabCompleter {

    public HelpCommand() {
        super("essentialsc.command.help");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        return handleCommand(player, player, args);
    }

    @Override
    protected boolean executeConsole(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("essentialsc.command.reload")) {
                sendNoPermission(sender, "essentialsc.command.reload");
                return true;
            }
            plugin.reloadConfig();
            EssentialsC.getLangManager().reload();
            TpsBarService tpsBarService = plugin.getTpsBarManager();
            if (tpsBarService != null) {
                tpsBarService.reloadSettings();
            }
            sender.sendMessage(getLang().getPrefixedString("messages.config-reloaded"));
            return true;
        }
        sender.sendMessage(getLang().getPrefixedString("messages.player-only"));
        return true;
    }

    private boolean handleCommand(CommandSender sender, Player player, String[] args) {
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("reload")) {
                if (!sender.hasPermission("essentialsc.command.reload")) {
                    sendNoPermission(sender, "essentialsc.command.reload");
                    return true;
                }
                plugin.reloadConfig();
                EssentialsC.getLangManager().reload();
                TpsBarService tpsBarService = plugin.getTpsBarManager();
                if (tpsBarService != null) {
                    tpsBarService.reloadSettings();
                }
                sender.sendMessage(getLang().getPrefixedString("messages.config-reloaded"));
                return true;
            }

            String actualCommand = getActualCommand(subCommand);
            BaseCommand targetCommand = CommandRegistry.getCommand(actualCommand);
            if (actualCommand != null && targetCommand != null) {
                String permission = CommandRegistry.getPermission(actualCommand);
                if (permission != null && !player.hasPermission(permission)) {
                    sendNoPermission(player, permission);
                    return true;
                }

                String[] forwardedArgs = Arrays.copyOfRange(args, 1, args.length);
                if (actualCommand.equals("seen") && forwardedArgs.length == 0) {
                        player.sendMessage(getLang().getPrefixedString("messages.seen-usage-console"));
                        return true;
                }
                targetCommand.execute(player, forwardedArgs);
                return true;
            }

            if (subCommand.equals("version") || subCommand.equals("v")) {
                player.sendMessage(getLang().getPrefixedString("messages.version",
                    Map.of("version", plugin.getDescription().getVersion())));
                player.sendMessage(getLang().getPrefixedString("messages.paper-version",
                    Map.of("version", Bukkit.getVersion())));
                return true;
            }

            player.sendMessage(getLang().getPrefixedString("messages.unknown-subcommand",
                Map.of("command", subCommand)));
            player.sendMessage(getLang().getPrefixedString("messages.help-usage"));
            return true;
        }

        LangManager lang = getLang();
        String version = plugin.getDescription().getVersion();

        sendPrefixed(player, lang.getString("help.title"));
        sendPrefixed(player, lang.getString("help.version", Map.of("version", version)));
        player.sendMessage("");

        boolean hasBlockCommands = false;
        StringBuilder blockCommands = new StringBuilder();

        if (CommandRegistry.isAvailable("workbench") && player.hasPermission("essentialsc.command.workbench")) {
            blockCommands.append(lang.getString("help.commands.workbench")).append("\n");
            hasBlockCommands = true;
        }
        if (CommandRegistry.isAvailable("anvil") && player.hasPermission("essentialsc.command.anvil")) {
            blockCommands.append(lang.getString("help.commands.anvil")).append("\n");
            hasBlockCommands = true;
        }
        if (CommandRegistry.isAvailable("cartographytable") && player.hasPermission("essentialsc.command.cartographytable")) {
            blockCommands.append(lang.getString("help.commands.cartographytable")).append("\n");
            hasBlockCommands = true;
        }
        if (CommandRegistry.isAvailable("grindstone") && player.hasPermission("essentialsc.command.grindstone")) {
            blockCommands.append(lang.getString("help.commands.grindstone")).append("\n");
            hasBlockCommands = true;
        }
        if (CommandRegistry.isAvailable("loom") && player.hasPermission("essentialsc.command.loom")) {
            blockCommands.append(lang.getString("help.commands.loom")).append("\n");
            hasBlockCommands = true;
        }
        if (CommandRegistry.isAvailable("smithingtable") && player.hasPermission("essentialsc.command.smithingtable")) {
            blockCommands.append(lang.getString("help.commands.smithingtable")).append("\n");
            hasBlockCommands = true;
        }
        if (CommandRegistry.isAvailable("stonecutter") && player.hasPermission("essentialsc.command.stonecutter")) {
            blockCommands.append(lang.getString("help.commands.stonecutter")).append("\n");
            hasBlockCommands = true;
        }
        if (CommandRegistry.isAvailable("enderchest") && player.hasPermission("essentialsc.command.enderchest")) {
            blockCommands.append(lang.getString("help.commands.enderchest")).append("\n");
            hasBlockCommands = true;
        }

        if (hasBlockCommands) {
            sendPrefixed(player, lang.getString("help.section-blocks"));
            sendPrefixedLines(player, blockCommands.toString().trim());
            player.sendMessage("");
        }

        boolean hasOtherCommands = false;
        StringBuilder otherCommands = new StringBuilder();

        if (CommandRegistry.isAvailable("hat") && player.hasPermission("essentialsc.command.hat")) {
            otherCommands.append(lang.getString("help.commands.hat")).append("\n");
            hasOtherCommands = true;
        }
        if (CommandRegistry.isAvailable("suicide") && player.hasPermission("essentialsc.command.suicide")) {
            otherCommands.append(lang.getString("help.commands.suicide")).append("\n");
            hasOtherCommands = true;
        }
        if (CommandRegistry.isAvailable("fly") && player.hasPermission("essentialsc.command.fly")) {
            otherCommands.append(lang.getString("help.commands.fly")).append("\n");
            hasOtherCommands = true;
        }
        if (CommandRegistry.isAvailable("nightvision") && player.hasPermission("essentialsc.command.nightvision")) {
            otherCommands.append(lang.getString("help.commands.nightvision")).append("\n");
            hasOtherCommands = true;
        }
        if (CommandRegistry.isAvailable("glow") && player.hasPermission("essentialsc.command.glow")) {
            otherCommands.append(lang.getString("help.commands.glow")).append("\n");
            hasOtherCommands = true;
        }
        if (CommandRegistry.isAvailable("heal") && player.hasPermission("essentialsc.command.heal")) {
            otherCommands.append(lang.getString("help.commands.heal")).append("\n");
            hasOtherCommands = true;
        }
        if (CommandRegistry.isAvailable("vanish") && player.hasPermission("essentialsc.command.vanish")) {
            otherCommands.append(lang.getString("help.commands.vanish")).append("\n");
            hasOtherCommands = true;
        }
        if (CommandRegistry.isAvailable("seen") && player.hasPermission("essentialsc.command.seen")) {
            otherCommands.append(lang.getString("help.commands.seen")).append("\n");
            hasOtherCommands = true;
        }
        if (CommandRegistry.isAvailable("admin") && player.hasPermission("essentialsc.command.admin")) {
            otherCommands.append(lang.getString("help.commands.admin")).append("\n");
            hasOtherCommands = true;
        }
        if (CommandRegistry.isAvailable("tpsbar") && player.hasPermission("essentialsc.command.tpsbar")) {
            otherCommands.append(lang.getString("help.commands.tpsbar")).append("\n");
            hasOtherCommands = true;
        }

        if (hasOtherCommands) {
            sendPrefixed(player, lang.getString("help.section-other"));
            sendPrefixedLines(player, otherCommands.toString().trim());
            player.sendMessage("");
        }

        sendPrefixed(player, lang.getString("help.footer"));
        return true;
    }

    private void sendNoPermission(CommandSender sender, String permission) {
        sender.sendMessage(getLang().getPrefixedString("messages.no-permission",
            Map.of("permission", permission)));
    }

    private void sendPrefixed(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    private void sendPrefixedLines(CommandSender sender, String message) {
        for (String line : message.split("\\R")) {
            sendPrefixed(sender, line);
        }
    }

    private String getActualCommand(String alias) {
        return CommandRegistry.resolveCommandName(alias);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();

            String[][] subCommands = {
                {"reload", "essentialsc.command.reload"},
                {"blocks", "essentialsc.command.blocks"},
                {"workbench", "essentialsc.command.workbench"},
                {"wb", "essentialsc.command.workbench"},
                {"anvil", "essentialsc.command.anvil"},
                {"cartographytable", "essentialsc.command.cartographytable"},
                {"cartography", "essentialsc.command.cartographytable"},
                {"ct", "essentialsc.command.cartographytable"},
                {"grindstone", "essentialsc.command.grindstone"},
                {"gs", "essentialsc.command.grindstone"},
                {"loom", "essentialsc.command.loom"},
                {"smithingtable", "essentialsc.command.smithingtable"},
                {"smithing", "essentialsc.command.smithingtable"},
                {"st", "essentialsc.command.smithingtable"},
                {"stonecutter", "essentialsc.command.stonecutter"},
                {"sc", "essentialsc.command.stonecutter"},
                {"enderchest", "essentialsc.command.enderchest"},
                {"ec", "essentialsc.command.enderchest"},
                {"hat", "essentialsc.command.hat"},
                {"suicide", "essentialsc.command.suicide"},
                {"die", "essentialsc.command.suicide"},
                {"fly", "essentialsc.command.fly"},
                {"nightvision", "essentialsc.command.nightvision"},
                {"nv", "essentialsc.command.nightvision"},
                {"glow", "essentialsc.command.glow"},
                {"heal", "essentialsc.command.heal"},
                {"vanish", "essentialsc.command.vanish"},
                {"v", "essentialsc.command.vanish"},
                {"seen", "essentialsc.command.seen"},
                {"info", "essentialsc.command.seen"},
                {"feed", "essentialsc.command.feed"},
                {"repair", "essentialsc.command.repair"},
                {"rep", "essentialsc.command.repair"},
                {"tpsbar", "essentialsc.command.tpsbar"},
                {"mobdrops", "essentialsc.mobdrops.enderman"},
                {"admin", "essentialsc.command.admin"},
                {"version", null},
                {"help", null}
            };

            for (String[] subCmd : subCommands) {
                if (!subCmd[0].startsWith(partial)) {
                    continue;
                }

                String actualCommand = getActualCommand(subCmd[0]);
                boolean available = actualCommand == null || CommandRegistry.isAvailable(actualCommand);
                if (available && (subCmd[1] == null || sender.hasPermission(subCmd[1]))) {
                    completions.add(subCmd[0]);
                }
            }

            return completions;
        }

        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if ((subCmd.equals("seen") || subCmd.equals("info")) && sender.hasPermission("essentialsc.command.seen")) {
                List<String> players = new ArrayList<>();
                String partial = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(partial)) {
                        players.add(p.getName());
                    }
                }
                return players;
            }

            if ((subCmd.equals("nightvision") || subCmd.equals("nv")) && sender.hasPermission("essentialsc.command.nightvision")) {
                return completeToggleArgs(args[1]);
            }

            if (subCmd.equals("glow") && sender.hasPermission("essentialsc.command.glow")) {
                return completeToggleArgs(args[1]);
            }

            if (subCmd.equals("tpsbar") && sender.hasPermission("essentialsc.command.tpsbar.others")) {
                List<String> players = new ArrayList<>();
                String partial = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(partial)) {
                        players.add(p.getName());
                    }
                }
                return players;
            }
        }

        return new ArrayList<>();
    }

    private List<String> completeToggleArgs(String partialInput) {
        List<String> completions = new ArrayList<>();
        String partial = partialInput.toLowerCase();
        for (String option : List.of("on", "off", "toggle")) {
            if (option.startsWith(partial)) {
                completions.add(option);
            }
        }
        return completions;
    }
}
