package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class StonecutterCommand extends BaseCommand {
    
    public StonecutterCommand() {
        super("essentialsc.command.stonecutter");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        // 使用 Paper API 打开切石机（标题跟随客户端语言）
        player.openStonecutter(null, true);
        return true;
    }
}
