package cn.infstar.essentialsC.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class SeenCommand extends BaseCommand {

    public SeenCommand() {
        super("essentialsc.command.seen");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(getLang().getPrefixedString("messages.seen-usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(getLang().getPrefixedString("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder info = new StringBuilder();
        info.append(getLang().getPrefix()).append(ChatColor.GOLD).append("玩家信息: ")
            .append(ChatColor.WHITE).append(target.getName()).append("\n");

        if (target.isOnline()) {
            info.append(ChatColor.GRAY).append("状态: ").append(ChatColor.GREEN).append("在线").append("\n");
            Player onlinePlayer = target.getPlayer();
            if (onlinePlayer != null) {
                info.append(ChatColor.GRAY).append("所在世界: ").append(ChatColor.WHITE)
                    .append(onlinePlayer.getWorld().getName()).append("\n");
            }
        } else {
            info.append(ChatColor.GRAY).append("状态: ").append(ChatColor.RED).append("离线").append("\n");
            long lastSeen = target.getLastSeen();
            if (lastSeen > 0) {
                info.append(ChatColor.GRAY).append("最后在线: ").append(ChatColor.WHITE)
                    .append(format.format(new Date(lastSeen))).append("\n");
            }
        }

        info.append(ChatColor.GRAY).append("首次加入: ").append(ChatColor.WHITE)
            .append(format.format(new Date(target.getFirstPlayed())));

        player.sendMessage(info.toString());
        return true;
    }
}
