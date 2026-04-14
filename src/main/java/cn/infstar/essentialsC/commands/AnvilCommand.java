package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class AnvilCommand extends BaseCommand {
    
    public AnvilCommand() {
        super("essentialsc.command.anvil");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        // 使用 Paper API 打开铁砧（标题跟随客户端语言）
        player.openAnvil(null, true);
        return true;
    }
}
