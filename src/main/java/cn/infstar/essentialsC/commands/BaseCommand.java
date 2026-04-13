package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.LangManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class BaseCommand implements CommandExecutor {
    
    protected String permission;
    protected static cn.infstar.essentialsC.EssentialsC plugin;
    
    public BaseCommand(String permission) {
        this.permission = permission;
        if (plugin == null) {
            plugin = cn.infstar.essentialsC.EssentialsC.getPlugin(cn.infstar.essentialsC.EssentialsC.class);
        }
    }
    
    public String getPermission() {
        return permission;
    }
    
    /**
     * 获取语言管理器
     */
    protected LangManager getLang() {
        return EssentialsC.getLangManager();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getLang().getString("messages.player-only"));
            return true;
        }
        
        if (!player.hasPermission(permission)) {
            String message = getLang().getString("messages.no-permission", 
                java.util.Map.of("permission", permission));
            player.sendMessage(message);
            return true;
        }
        
        return execute(player, args);
    }
    
    protected abstract boolean execute(Player player, String[] args);
}
