package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class LoomCommand extends BaseCommand {
    
    public LoomCommand() {
        super("essentialsc.command.loom");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        // 使用 Paper API 打开织布机（标题跟随客户端语言）
        player.openLoom(null, true);
        return true;
    }
}
