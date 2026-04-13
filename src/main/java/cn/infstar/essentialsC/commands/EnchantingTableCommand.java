package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class EnchantingTableCommand extends BaseCommand {
    
    public EnchantingTableCommand() {
        super("essentialsc.command.enchantingtable");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        player.openEnchanting(null, true);
        player.sendMessage(getLang().getString("enchantingtable-opened"));
        return true;
    }
}
