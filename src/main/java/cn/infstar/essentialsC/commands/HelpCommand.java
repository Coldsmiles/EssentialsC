package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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
                sender.sendMessage(getLang().getString("messages.no-permission"));
                return true;
            }
            plugin.reloadConfig();
            EssentialsC.getLangManager().reload();
            sender.sendMessage(getLang().getString("prefix") + "Configuration reloaded.");
            return true;
        }
        sender.sendMessage(getLang().getString("messages.player-only"));
        return true;
    }

    private boolean handleCommand(CommandSender sender, Player player, String[] args) {
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("reload")) {
                if (!sender.hasPermission("essentialsc.command.reload")) {
                    sender.sendMessage(getLang().getString("messages.no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                EssentialsC.getLangManager().reload();
                sender.sendMessage(getLang().getString("prefix") + "Configuration reloaded.");
                return true;
            }

            String actualCommand = getActualCommand(subCommand);
            BaseCommand targetCommand = CommandRegistry.getCommand(actualCommand);
            if (actualCommand != null && targetCommand != null) {
                String permission = CommandRegistry.getPermission(actualCommand);
                if (permission != null && !player.hasPermission(permission)) {
                    player.sendMessage(getLang().getString("messages.no-permission"));
                    return true;
                }

                if (actualCommand.equals("seen")) {
                    if (args.length < 2) {
                        player.sendMessage(getLang().getString("prefix") + getLang().getString("messages.seen-usage-console"));
                        return true;
                    }
                    targetCommand.execute(player, new String[]{args[1]});
                } else {
                    targetCommand.execute(player, new String[0]);
                }
                return true;
            }

            if (subCommand.equals("version") || subCommand.equals("v")) {
                player.sendMessage(getLang().getString("prefix") + "EssentialsC v" + plugin.getDescription().getVersion());
                player.sendMessage(getLang().getString("prefix") + "Running on Paper " + Bukkit.getVersion());
                return true;
            }

            player.sendMessage(getLang().getString("prefix") + getLang().getString("messages.unknown-subcommand",
                Map.of("command", subCommand)));
            player.sendMessage(getLang().getString("prefix") + getLang().getString("messages.help-usage"));
            return true;
        }

        LangManager lang = getLang();
        String version = plugin.getDescription().getVersion();

        player.sendMessage(lang.getString("help.title"));
        player.sendMessage(lang.getString("help.version", Map.of("version", version)));
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
            player.sendMessage(lang.getString("help.section-blocks"));
            player.sendMessage(blockCommands.toString().trim());
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

        if (hasOtherCommands) {
            player.sendMessage(lang.getString("help.section-other"));
            player.sendMessage(otherCommands.toString().trim());
            player.sendMessage("");
        }

        player.sendMessage(lang.getString("help.footer"));
        return true;
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
                {"heal", "essentialsc.command.heal"},
                {"vanish", "essentialsc.command.vanish"},
                {"v", "essentialsc.command.vanish"},
                {"seen", "essentialsc.command.seen"},
                {"info", "essentialsc.command.seen"},
                {"feed", "essentialsc.command.feed"},
                {"repair", "essentialsc.command.repair"},
                {"rep", "essentialsc.command.repair"},
                {"mobdrops", "essentialsc.mobdrops.enderman"},
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
        }

        return new ArrayList<>();
    }
}
