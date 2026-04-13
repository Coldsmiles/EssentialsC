package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishCommand extends BaseCommand {
    
    private static final Set<UUID> vanishedPlayers = new HashSet<>();
    
    public VanishCommand() {
        super("essentialsc.command.vanish");
    }
    
    @Override
    protected boolean execute(@NotNull Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        
        if (vanishedPlayers.contains(uuid)) {
            // 取消隐身
            vanishedPlayers.remove(uuid);
            showPlayerToAll(player);
            player.sendMessage(getLang().getString("messages.vanish-disabled"));
        } else {
            // 开启隐身
            vanishedPlayers.add(uuid);
            hidePlayerFromAll(player);
            player.sendMessage(getLang().getString("messages.vanish-enabled"));
        }
        return true;
    }
    
    private void hidePlayerFromAll(Player player) {
        for (Player online : player.getServer().getOnlinePlayers()) {
            if (online != player) {
                online.hidePlayer(plugin, player);
            }
        }
    }
    
    private void showPlayerToAll(Player player) {
        for (Player online : player.getServer().getOnlinePlayers()) {
            if (online != player) {
                online.showPlayer(plugin, player);
            }
        }
    }
    
    public static boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }
}
