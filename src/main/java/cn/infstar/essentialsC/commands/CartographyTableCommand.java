package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class CartographyTableCommand extends BaseCommand {
    
    public CartographyTableCommand() {
        super("essentialsc.command.cartographytable");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        // 使用 Paper API 打开制图台（标题跟随客户端语言）
        player.openCartographyTable(null, true);
        return true;
    }
}
