package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelpCommand extends BaseCommand implements TabCompleter {
    
    // 缓存命令实例，避免重复创建
    static final java.util.Map<String, BaseCommand> COMMAND_CACHE = new java.util.HashMap<>();
    
    static {
        COMMAND_CACHE.put("workbench", new WorkbenchCommand());
        COMMAND_CACHE.put("anvil", new AnvilCommand());
        COMMAND_CACHE.put("cartographytable", new CartographyTableCommand());
        COMMAND_CACHE.put("grindstone", new GrindstoneCommand());
        COMMAND_CACHE.put("loom", new LoomCommand());
        COMMAND_CACHE.put("smithingtable", new SmithingTableCommand());
        COMMAND_CACHE.put("stonecutter", new StonecutterCommand());
        COMMAND_CACHE.put("enderchest", new EnderChestCommand());
        COMMAND_CACHE.put("hat", new HatCommand());
        COMMAND_CACHE.put("suicide", new SuicideCommand());
        COMMAND_CACHE.put("fly", new FlyCommand());
        COMMAND_CACHE.put("heal", new HealCommand());
        COMMAND_CACHE.put("vanish", new VanishCommand());
        COMMAND_CACHE.put("seen", new SeenCommand());
        COMMAND_CACHE.put("feed", new FeedCommand());
        COMMAND_CACHE.put("repair", new RepairCommand());
        COMMAND_CACHE.put("blocks", new BlocksMenuCommand());
        COMMAND_CACHE.put("mobdrops", new MobDropCommand());
    }
    
    public HelpCommand() {
        super("essentialsc.command.help");
    }
    
    @Override
    protected boolean execute(@NotNull Player player, String[] args) {
        return handleCommand(player, player, args);
    }
    
    @Override
    protected boolean executeConsole(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("essentialsc.command.reload")) {
                sender.sendMessage(getLang().getString("messages.no-permission"));
                return true;
            }
            plugin.reloadConfig();
            EssentialsC.getLangManager().reload();
            sender.sendMessage(getLang().getString("prefix") + "§a配置已重载！");
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
                sender.sendMessage(getLang().getString("prefix") + "§a配置已重载！");
                return true;
            }
            // 功能方块和其他命令 - 使用别名映射
            String actualCommand = getActualCommand(subCommand);
            if (actualCommand != null && COMMAND_CACHE.containsKey(actualCommand)) {
                String permission = getPermissionForCommand(actualCommand);
                if (!player.hasPermission(permission)) {
                    player.sendMessage(getLang().getString("messages.no-permission"));
                    return true;
                }
                
                // seen 需要特殊处理参数
                if (actualCommand.equals("seen")) {
                    if (args.length < 2) {
                        player.sendMessage(getLang().getString("prefix") + getLang().getString("messages.seen-usage-console"));
                        return true;
                    }
                    COMMAND_CACHE.get("seen").execute(player, new String[]{args[1]});
                } else {
                    COMMAND_CACHE.get(actualCommand).execute(player, new String[]{});
                }
                return true;
            } else if (subCommand.equals("version") || subCommand.equals("v")) {
                player.sendMessage(getLang().getString("prefix") + "§6EssentialsC §fv" + plugin.getDescription().getVersion());
                player.sendMessage(getLang().getString("prefix") + "§7运行在 Paper " + Bukkit.getVersion());
                return true;
            } else {
                player.sendMessage(getLang().getString("prefix") + getLang().getString("messages.unknown-subcommand",
                    java.util.Map.of("command", subCommand)));
                player.sendMessage(getLang().getString("prefix") + getLang().getString("messages.help-usage"));
                return true;
            }
        }
        
        // 显示帮助
        LangManager lang = getLang();
        String version = plugin.getDescription().getVersion();
        
        player.sendMessage(lang.getString("help.title"));
        player.sendMessage(lang.getString("help.version", 
            java.util.Map.of("version", version)));
        player.sendMessage("");
        
        // 功能方块命令（检查权限后显示）
        boolean hasBlockCommands = false;
        StringBuilder blockCommands = new StringBuilder();
        
        if (player.hasPermission("essentialsc.command.workbench")) {
            blockCommands.append(lang.getString("help.commands.workbench")).append("\n");
            hasBlockCommands = true;
        }
        if (player.hasPermission("essentialsc.command.anvil")) {
            blockCommands.append(lang.getString("help.commands.anvil")).append("\n");
            hasBlockCommands = true;
        }
        if (player.hasPermission("essentialsc.command.cartographytable")) {
            blockCommands.append(lang.getString("help.commands.cartographytable")).append("\n");
            hasBlockCommands = true;
        }
        if (player.hasPermission("essentialsc.command.grindstone")) {
            blockCommands.append(lang.getString("help.commands.grindstone")).append("\n");
            hasBlockCommands = true;
        }
        if (player.hasPermission("essentialsc.command.loom")) {
            blockCommands.append(lang.getString("help.commands.loom")).append("\n");
            hasBlockCommands = true;
        }
        if (player.hasPermission("essentialsc.command.smithingtable")) {
            blockCommands.append(lang.getString("help.commands.smithingtable")).append("\n");
            hasBlockCommands = true;
        }
        if (player.hasPermission("essentialsc.command.stonecutter")) {
            blockCommands.append(lang.getString("help.commands.stonecutter")).append("\n");
            hasBlockCommands = true;
        }
        if (player.hasPermission("essentialsc.command.enderchest")) {
            blockCommands.append(lang.getString("help.commands.enderchest")).append("\n");
            hasBlockCommands = true;
        }
        
        if (hasBlockCommands) {
            player.sendMessage(lang.getString("help.section-blocks"));
            player.sendMessage(blockCommands.toString().trim());
            player.sendMessage("");
        }
        
        // 其他命令（检查权限后显示）
        boolean hasOtherCommands = false;
        StringBuilder otherCommands = new StringBuilder();
        
        if (player.hasPermission("essentialsc.command.hat")) {
            otherCommands.append(lang.getString("help.commands.hat")).append("\n");
            hasOtherCommands = true;
        }
        if (player.hasPermission("essentialsc.command.suicide")) {
            otherCommands.append(lang.getString("help.commands.suicide")).append("\n");
            hasOtherCommands = true;
        }
        if (player.hasPermission("essentialsc.command.fly")) {
            otherCommands.append(lang.getString("help.commands.fly")).append("\n");
            hasOtherCommands = true;
        }
        if (player.hasPermission("essentialsc.command.heal")) {
            otherCommands.append(lang.getString("help.commands.heal")).append("\n");
            hasOtherCommands = true;
        }
        if (player.hasPermission("essentialsc.command.vanish")) {
            otherCommands.append(lang.getString("help.commands.vanish")).append("\n");
            hasOtherCommands = true;
        }
        if (player.hasPermission("essentialsc.command.seen")) {
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
    
    /**
     * 将别名映射到实际命令名
     */
    private String getActualCommand(String alias) {
        return switch (alias) {
            case "wb" -> "workbench";
            case "cartography", "ct" -> "cartographytable";
            case "gs" -> "grindstone";
            case "smithing", "st" -> "smithingtable";
            case "sc" -> "stonecutter";
            case "ec" -> "enderchest";
            case "die" -> "suicide";
            case "info" -> "seen";
            case "rep" -> "repair";
            default -> alias;
        };
    }
    
    /**
     * 获取命令对应的权限节点
     */
    private String getPermissionForCommand(String command) {
        return "essentialsc.command." + command;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            
            // 所有可能的子命令及其权限（包括别名）
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
                if (subCmd[0].startsWith(partial)) {
                    if (subCmd[1] == null || sender.hasPermission(subCmd[1])) {
                        completions.add(subCmd[0]);
                    }
                }
            }
            
            return completions;
        } else if (args.length == 2) {
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
