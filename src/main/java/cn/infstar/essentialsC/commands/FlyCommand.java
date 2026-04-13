package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class FlyCommand extends BaseCommand {
    
    public FlyCommand() {
        super("essentialsc.command.fly");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        boolean currentFlyState = player.getAllowFlight();
        
        if (currentFlyState) {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(getLang().getString("messages.fly-disabled"));
        } else {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.sendMessage(getLang().getString("messages.fly-enabled"));
        }
        
        return true;
    }
}
