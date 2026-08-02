package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.commands.MobDropCommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class MobDropMenuListener implements Listener {

    private final EssentialsC plugin;

    public MobDropMenuListener(EssentialsC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof MobDropCommand.MobDropMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        if (event.getRawSlot() == MobDropCommand.getEndermanSlot()) {
            if (!player.hasPermission("essentialsc.mobdrops.enderman")) {
                player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.no-permission",
                    Map.of("permission", "essentialsc.mobdrops.enderman")));
                player.closeInventory();
                return;
            }
            toggleEndermanDrops(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> MobDropCommand.openMobDropMenu(plugin, player), 2L);
        }
    }

    private void toggleEndermanDrops(Player player) {
        FileConfiguration config = plugin.getConfig();
        boolean newValue = !config.getBoolean("mob-drops.enderman.allow-drops", true);
        if (!plugin.getFeatureConfigManager().updateMainConfigValue(
            "mob-drops.enderman.allow-drops", newValue)) {
            player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.mobdrop-save-failed",
                Map.of("error", "无法写入 config.yml")));
            return;
        }

        String status = EssentialsC.getLangManager().getString(newValue
            ? "mobdrops-menu.status.enabled"
            : "mobdrops-menu.status.disabled");
        player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.mobdrop-toggled",
            Map.of("status", status)));
    }
}
