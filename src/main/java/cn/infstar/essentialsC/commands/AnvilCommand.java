package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class AnvilCommand extends BaseCommand {
    
    public AnvilCommand() {
        super("essentialsc.command.anvil");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        player.openAnvil(null, true);
        player.sendMessage(getLang().getString("anvil-opened"));
        return true;
    }
}
