package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class SmithingTableCommand extends BaseCommand {

    public SmithingTableCommand() {
        super("essentialsc.command.smithingtable");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openInventory(MenuType.SMITHING.builder().checkReachable(false).build(player));
        playBlockShortcutSound(player, Material.SMITHING_TABLE, Sound.BLOCK_SMITHING_TABLE_USE);
        return true;
    }
}
