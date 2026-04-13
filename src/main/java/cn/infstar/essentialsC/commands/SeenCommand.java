package cn.infstar.essentialsC.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class SeenCommand extends BaseCommand {
    
    public SeenCommand() {
        super("essentialsc.command.seen");
    }
    
    @Override
    protected boolean execute(@NotNull Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(getLang().getString("messages.seen-usage"));
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(getLang().getString("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }
        
        StringBuilder info = new StringBuilder();
        info.append("§6========== §e玩家信息 §6==========\n");
        info.append("§7玩家名称: §f").append(target.getName()).append("\n");
        
        if (target.isOnline()) {
            info.append("§7状态: §a在线\n");
            Player onlinePlayer = target.getPlayer();
            if (onlinePlayer != null) {
                info.append("§7所在世界: §f").append(onlinePlayer.getWorld().getName()).append("\n");
            }
        } else {
            info.append("§7状态: §c离线\n");
            long lastSeen = target.getLastSeen();
            if (lastSeen > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                info.append("§7最后上线: §f").append(sdf.format(new Date(lastSeen))).append("\n");
            }
        }
        
        info.append("§7首次加入: §f").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(target.getFirstPlayed()))).append("\n");
        info.append("§6=============================");
        
        player.sendMessage(info.toString());
        return true;
    }
}
