package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class HatCommand extends BaseCommand {

    public HatCommand() {
        super("essentialsc.command.hat");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (handItem == null || handItem.isEmpty()) {
            player.sendMessage(getLang().getPrefixedString("messages.hat-no-item"));
            return true;
        }

        ItemStack helmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(handItem);
        player.getInventory().setItemInMainHand(helmet == null || helmet.isEmpty() ? null : helmet);
        player.updateInventory();

        player.sendMessage(getLang().getPrefixedString("messages.hat-success",
            Map.of("item", handItem.getType().toString())));
        return true;
    }
}
