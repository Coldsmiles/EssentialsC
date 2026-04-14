package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.listeners.InventoryTitleListener;
import org.bukkit.entity.Player;

/**
 * 末影箱命令 - 使用 ProtocolLib 实现自定义标题
 */
public class EnderChestCommand extends BaseCommand {
    
    public EnderChestCommand() {
        super("essentialsc.command.enderchest");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        InventoryTitleListener titleListener = EssentialsC.getInventoryTitleListener();
        
        // 如果 ProtocolLib 可用，设置自定义标题
        if (titleListener != null) {
            String title = "&5随身末影箱";
            titleListener.markForTitleChange(player, title);
        }
        
        // 打开末影箱
        player.openInventory(player.getEnderChest());
        return true;
    }
}
