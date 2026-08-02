package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class LoomCommand extends BaseCommand {

    public LoomCommand() {
        super("essentialsc.command.loom");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openInventory(MenuType.LOOM.builder().checkReachable(false).build(player));
        playBlockShortcutSound(player, Material.LOOM, Sound.UI_LOOM_SELECT_PATTERN);
        return true;
    }
}
