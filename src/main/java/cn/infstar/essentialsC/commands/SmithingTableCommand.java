package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class SmithingTableCommand extends BaseCommand {
    
    public SmithingTableCommand() {
        super("essentialsc.command.smithingtable");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        // 使用 Paper API 打开锻造台（标题跟随客户端语言）
        player.openSmithingTable(null, true);
        return true;
    }
}
