package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class StonecutterCommand extends BaseCommand {

    public StonecutterCommand() {
        super("essentialsc.command.stonecutter");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openInventory(MenuType.STONECUTTER.builder().checkReachable(false).build(player));
        playBlockShortcutSound(player, Material.STONECUTTER, Sound.UI_STONECUTTER_SELECT_RECIPE);
        return true;
    }
}
