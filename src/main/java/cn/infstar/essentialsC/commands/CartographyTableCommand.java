package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class CartographyTableCommand extends BaseCommand {

    public CartographyTableCommand() {
        super("essentialsc.command.cartographytable");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openCartographyTable(null, true);
        playBlockShortcutSound(player, Material.CARTOGRAPHY_TABLE, Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER);
        return true;
    }
}
