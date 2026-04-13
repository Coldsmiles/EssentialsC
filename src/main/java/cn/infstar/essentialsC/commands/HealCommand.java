package cn.infstar.essentialsC.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class HealCommand extends BaseCommand {
    
    public HealCommand() {
        super("essentialsc.command.heal");
    }
    
    @Override
    protected boolean execute(@NotNull Player player, String[] args) {
        if (args.length == 0) {
            // 治疗自己
            healPlayer(player);
            player.sendMessage(getLang().getString("messages.heal-self"));
        } else {
            // 检查是否有治疗他人的权限
            if (!player.hasPermission("essentialsc.command.heal.others")) {
                player.sendMessage(getLang().getString("messages.no-permission-others"));
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(getLang().getString("messages.player-not-found", Map.of("player", args[0])));
                return true;
            }
            
            healPlayer(target);
            player.sendMessage(getLang().getString("messages.heal-other", Map.of("player", target.getName())));
            target.sendMessage(getLang().getString("messages.heal-by-other", Map.of("admin", player.getName())));
        }
        return true;
    }
    
    private void healPlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.clearActivePotionEffects();
        player.setFireTicks(0);
    }
}
