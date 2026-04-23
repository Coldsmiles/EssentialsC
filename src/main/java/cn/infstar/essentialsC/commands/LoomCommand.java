package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class LoomCommand extends BaseCommand {

    public LoomCommand() {
        super("essentialsc.command.loom");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openLoom(null, true);
        playBlockShortcutSound(player, Material.LOOM, Sound.UI_LOOM_SELECT_PATTERN);
        return true;
    }
}
