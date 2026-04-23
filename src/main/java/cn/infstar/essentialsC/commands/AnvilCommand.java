package cn.infstar.essentialsC.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class AnvilCommand extends BaseCommand {

    public AnvilCommand() {
        super("essentialsc.command.anvil");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        player.openAnvil(null, true);
        playBlockShortcutSound(player, Material.ANVIL, Sound.BLOCK_ANVIL_USE);
        return true;
    }
}
