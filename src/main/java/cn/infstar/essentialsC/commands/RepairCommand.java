package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

public class RepairCommand extends BaseCommand {
    
    public RepairCommand() {
        super("essentialsc.command.repair");
    }
    
    @Override
    protected boolean execute(@NotNull Player player, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            // 检查是否有修复全部的权限
            if (!player.hasPermission("essentialsc.command.repair.all")) {
                player.sendMessage(getLang().getString("messages.no-permission-repair-all"));
                return true;
            }
            
            int repairedCount = repairAll(player);
            if (repairedCount > 0) {
                player.sendMessage(getLang().getString("messages.repair-all-success", java.util.Map.of("count", String.valueOf(repairedCount))));
            } else {
                player.sendMessage(getLang().getString("messages.repair-no-items"));
            }
        } else {
            // 修复手中物品
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                player.sendMessage(getLang().getString("messages.repair-no-item-in-hand"));
                return true;
            }
            
            if (repairItem(item)) {
                player.sendMessage(getLang().getString("messages.repair-hand-success"));
            } else {
                player.sendMessage(getLang().getString("messages.repair-not-damaged"));
            }
        }
        return true;
    }
    
    private boolean repairItem(ItemStack item) {
        if (item.getItemMeta() instanceof Damageable damageable) {
            if (damageable.hasDamage()) {
                Damageable newMeta = (Damageable) damageable.clone();
                newMeta.setDamage(0);
                item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) newMeta);
                return true;
            }
        }
        return false;
    }
    
    private int repairAll(Player player) {
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();
        
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                if (repairItem(item)) {
                    count++;
                }
            }
        }
        
        // 也修复盔甲栏
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            if (item != null && !item.getType().isAir()) {
                if (repairItem(item)) {
                    count++;
                }
            }
        }
        
        player.updateInventory();
        return count;
    }
}
