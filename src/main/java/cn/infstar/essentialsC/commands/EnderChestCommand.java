package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class EnderChestCommand extends BaseCommand {

    public EnderChestCommand() {
        super("essentialsc.command.enderchest");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openInventory(player.getEnderChest());
        playBlockShortcutSound(player, Material.ENDER_CHEST, Sound.BLOCK_ENDER_CHEST_OPEN);
        return true;
    }
}
