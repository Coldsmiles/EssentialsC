package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class FlyCommand extends BaseCommand {
    
    public FlyCommand() {
        super("essentialsc.command.fly");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        boolean currentFlyState = plugin.getPlayerStateManager().isFlyEnabled(player);
        
        if (currentFlyState) {
            plugin.getPlayerStateManager().disableFly(player);
            player.sendMessage(getLang().getPrefixedString("messages.fly-disabled"));
        } else {
            plugin.getPlayerStateManager().enableFly(player);
            player.sendMessage(getLang().getPrefixedString("messages.fly-enabled"));
        }
        
        return true;
    }
}
