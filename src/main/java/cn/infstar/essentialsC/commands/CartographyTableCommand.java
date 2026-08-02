package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class CartographyTableCommand extends BaseCommand {

    public CartographyTableCommand() {
        super("essentialsc.command.cartographytable");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openInventory(MenuType.CARTOGRAPHY_TABLE.builder().checkReachable(false).build(player));
        playBlockShortcutSound(player, Material.CARTOGRAPHY_TABLE, Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER);
        return true;
    }
}
