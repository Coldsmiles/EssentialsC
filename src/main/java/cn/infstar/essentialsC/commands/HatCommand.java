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
            player.sendMessage(getLang().getString("messages.hat-no-item"));
            return true;
        }
        
        ItemStack helmet = player.getInventory().getHelmet();
        
        // 如果头盔栏有物品，先放回背包
        if (helmet != null && !helmet.isEmpty()) {
            player.getInventory().setHelmet(handItem);
            player.getInventory().setItemInMainHand(helmet);
        } else {
            player.getInventory().setHelmet(handItem);
            player.getInventory().setItemInMainHand(null);
        }
        
        player.updateInventory();
        
        String itemName = handItem.getType().toString();
        String message = getLang().getString("messages.hat-success", 
            Map.of("item", itemName));
        player.sendMessage(message);
        return true;
    }
}
