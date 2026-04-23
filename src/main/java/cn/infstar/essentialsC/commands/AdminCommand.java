package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class AdminCommand extends BaseCommand {

    public AdminCommand() {
        super("essentialsc.command.admin");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        plugin.getAdminModeManager().toggle(player);
        return true;
    }
}
