package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SmithingTableCommand extends BaseCommand {

    public SmithingTableCommand() {
        super("essentialsc.command.smithingtable");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openSmithingTable(null, true);
        playBlockShortcutSound(player, Material.SMITHING_TABLE, Sound.BLOCK_SMITHING_TABLE_USE);
        return true;
    }
}
