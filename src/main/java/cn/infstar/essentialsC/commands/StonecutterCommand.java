package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class StonecutterCommand extends BaseCommand {

    public StonecutterCommand() {
        super("essentialsc.command.stonecutter");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openStonecutter(null, true);
        playBlockShortcutSound(player, Material.STONECUTTER, Sound.UI_STONECUTTER_SELECT_RECIPE);
        return true;
    }
}
