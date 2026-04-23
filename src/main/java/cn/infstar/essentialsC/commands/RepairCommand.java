package cn.infstar.essentialsC.commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Map;

public class RepairCommand extends BaseCommand {

    public RepairCommand() {
        super("essentialsc.command.repair");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            if (!player.hasPermission("essentialsc.command.repair.all")) {
                player.sendMessage(getLang().getPrefixedString("messages.no-permission-repair-all"));
                return true;
            }

            int repairedCount = repairAll(player);
            if (repairedCount > 0) {
                player.sendMessage(getLang().getPrefixedString("messages.repair-all-success",
                    Map.of("count", String.valueOf(repairedCount))));
            } else {
                player.sendMessage(getLang().getPrefixedString("messages.repair-no-items"));
            }
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage(getLang().getPrefixedString("messages.repair-no-item-in-hand"));
            return true;
        }

        if (repairItem(item)) {
            player.sendMessage(getLang().getPrefixedString("messages.repair-hand-success"));
        } else {
            player.sendMessage(getLang().getPrefixedString("messages.repair-not-damaged"));
        }
        return true;
    }

    private boolean repairItem(ItemStack item) {
        if (item.getItemMeta() instanceof Damageable damageable && damageable.hasDamage()) {
            Damageable newMeta = (Damageable) damageable.clone();
            newMeta.setDamage(0);
            item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) newMeta);
            return true;
        }
        return false;
    }

    private int repairAll(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir() && repairItem(item)) {
                count++;
            }
        }

        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && !item.getType().isAir() && repairItem(item)) {
                count++;
            }
        }

        player.updateInventory();
        return count;
    }
}
