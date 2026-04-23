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
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
            toggleEndermanDrops(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> MobDropCommand.openMobDropMenu(plugin, player), 2L);
        }
    }

    private void toggleEndermanDrops(Player player) {
        FileConfiguration config = plugin.getConfig();
        boolean newValue = !config.getBoolean("mob-drops.enderman.enabled", true);
        config.set("mob-drops.enderman.enabled", newValue);

        try {
            config.save(plugin.getDataFolder().toPath().resolve("config.yml").toFile());
        } catch (Exception e) {
            player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.mobdrop-save-failed",
                Map.of("error", e.getMessage())));
            return;
        }

        String status = EssentialsC.getLangManager().getString(newValue
            ? "mobdrops-menu.status.enabled"
            : "mobdrops-menu.status.disabled");
        player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.mobdrop-toggled",
            Map.of("status", status)));
    }
}
