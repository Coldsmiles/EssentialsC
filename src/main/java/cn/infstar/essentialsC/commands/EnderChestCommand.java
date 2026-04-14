package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

/**
 * 末影箱命令 - 参考 EssentialsX 实现
 * 直接打开玩家的末影箱，确保数据安全
 */
public class EnderChestCommand extends BaseCommand {
    
    public EnderChestCommand() {
        super("essentialsc.command.enderchest");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        // 直接打开玩家的末影箱（EssentialsX 方式）
        // 优点：100% 安全，不会吞物品或刷物品
        // 缺点：标题显示为 "Ender Chest"（由客户端语言决定）
        player.openInventory(player.getEnderChest());
        return true;
    }
}
