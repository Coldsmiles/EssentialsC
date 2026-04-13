package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.LangManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HelpCommand extends BaseCommand {
    
    public HelpCommand() {
        super("essentialsc.command.help");
    }
    
    @Override
    protected boolean execute(@NotNull Player player, String[] args) {
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
        if (player.hasPermission("essentialsc.command.enchantingtable")) {
            blockCommands.append(lang.getString("help.commands.enchantingtable")).append("\n");
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
        if (player.hasPermission("essentialsc.command.admin")) {
            otherCommands.append(lang.getString("help.commands.admin")).append("\n");
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
}
