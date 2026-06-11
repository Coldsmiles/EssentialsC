package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class WorkbenchCommand extends BaseCommand {

    public WorkbenchCommand() {
        super("essentialsc.command.workbench");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openWorkbench(null, true);
        playBlockShortcutSound(player, Material.CRAFTING_TABLE, Sound.UI_BUTTON_CLICK);
        return true;
    }
}
