package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class GrindstoneCommand extends BaseCommand {

    public GrindstoneCommand() {
        super("essentialsc.command.grindstone");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openGrindstone(null, true);
        playBlockShortcutSound(player, Material.GRINDSTONE, Sound.BLOCK_GRINDSTONE_USE);
        return true;
    }
}
