package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

import java.util.Map;

public class SuicideCommand extends BaseCommand {
    
    public SuicideCommand() {
        super("essentialsc.command.suicide");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        String message = getLang().getString("messages.suicide-message", 
            Map.of("player", player.getName()));
        player.setHealth(0);
        // 消息会在玩家死亡后显示，所以这里不发送
        return true;
    }
}
