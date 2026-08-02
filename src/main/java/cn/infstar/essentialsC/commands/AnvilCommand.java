package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class AnvilCommand extends BaseCommand {

    public AnvilCommand() {
        super("essentialsc.command.anvil");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openInventory(MenuType.ANVIL.builder().checkReachable(false).build(player));
        playBlockShortcutSound(player, Material.ANVIL, Sound.BLOCK_ANVIL_USE);
        return true;
    }
}
