package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.entity.Player;

public class EnderChestCommand extends BaseCommand {
    
    public EnderChestCommand() {
        super("essentialsc.command.enderchest");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        EssentialsC plugin = EssentialsC.getInstance();
        
        // 如果启用了 ProtocolLib，使用自定义标题
        if (plugin.isProtocolLibEnabled()) {
            // 从配置读取标题
            String title = plugin.getConfig().getString("enderchest.title", "&5随身末影箱");
            
            // 标记下一个打开的 inventory 需要修改标题
            plugin.getInventoryTitleListener().markForTitleChange(player, title);
        }
        
        // 直接打开玩家的末影箱（参考 EssentialsX 实现）
        player.openInventory(player.getEnderChest());
        return true;
    }
}
