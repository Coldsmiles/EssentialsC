package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class GrindstoneCommand extends BaseCommand {
    
    public GrindstoneCommand() {
        super("essentialsc.command.grindstone");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        // 使用 Paper API 打开砂轮（标题跟随客户端语言）
        player.openGrindstone(null, true);
        return true;
    }
}
