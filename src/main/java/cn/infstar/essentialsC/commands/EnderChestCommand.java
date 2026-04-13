package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class EnderChestCommand extends BaseCommand {
    
    public EnderChestCommand() {
        super("essentialsc.command.enderchest");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        // 打开玩家的末影箱（标题由客户端决定）
        player.openInventory(player.getEnderChest());
        return true;
    }
}
