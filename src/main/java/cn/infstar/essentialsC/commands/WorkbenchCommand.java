package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;

public class WorkbenchCommand extends BaseCommand {
    
    public WorkbenchCommand() {
        super("essentialsc.command.workbench");
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        // 打开工作台（标题由客户端语言决定）
        player.openWorkbench(null, true);
        return true;
    }
}
