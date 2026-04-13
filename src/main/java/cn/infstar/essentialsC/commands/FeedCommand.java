package cn.infstar.essentialsC.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class FeedCommand extends BaseCommand {
    
    public FeedCommand() {
        super("essentialsc.command.feed");
    }
    
    @Override
    protected boolean execute(@NotNull Player player, String[] args) {
        if (args.length == 0) {
            // 喂饱自己
            feedPlayer(player);
            player.sendMessage(getLang().getString("messages.feed-self"));
        } else {
            // 检查是否有喂饱他人的权限
            if (!player.hasPermission("essentialsc.command.feed.others")) {
                player.sendMessage(getLang().getString("messages.no-permission-others"));
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(getLang().getString("messages.player-not-found", Map.of("player", args[0])));
                return true;
            }
            
            feedPlayer(target);
            player.sendMessage(getLang().getString("messages.feed-other", Map.of("player", target.getName())));
            target.sendMessage(getLang().getString("messages.feed-by-other", Map.of("admin", player.getName())));
        }
        return true;
    }
    
    private void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }
}
