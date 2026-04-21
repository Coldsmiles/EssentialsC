package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MobDropMenuListener implements Listener {
    
    private final EssentialsC plugin;
    
    public MobDropMenuListener(EssentialsC plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof cn.infstar.essentialsC.commands.MobDropCommand.MobDropMenuHolder)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) {
            return;
        }
        
        if (event.getSlot() == 13) {
            toggleEndermanDrops(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> openMobDropMenu(player), 2L);
        }
    }
    
    private void toggleEndermanDrops(Player player) {
        FileConfiguration config = plugin.getConfig();
        boolean currentValue = config.getBoolean("mob-drops.enderman.enabled", true);
        boolean newValue = !currentValue;
        
        config.set("mob-drops.enderman.enabled", newValue);
        
        try {
            config.save(plugin.getDataFolder().toPath().resolve("config.yml").toFile());
        } catch (Exception e) {
            player.sendMessage(EssentialsC.getLangManager().getString("prefix") + 
                EssentialsC.getLangManager().getString("messages.mobdrop-save-failed",
                    java.util.Map.of("error", e.getMessage())));
            return;
        }
        
        String status = newValue ? "§a开启" : "§c关闭";
        player.sendMessage(EssentialsC.getLangManager().getString("prefix") + 
            EssentialsC.getLangManager().getString("messages.mobdrop-toggled",
                java.util.Map.of("status", status)));
    }
    
    private void openMobDropMenu(Player player) {
        boolean endermanEnabled = plugin.getConfig().getBoolean("mob-drops.enderman.enabled", true);
        
        Inventory menu = new cn.infstar.essentialsC.commands.MobDropCommand.MobDropMenuHolder("§6§l生物掉落控制").getInventory();
        
        ItemStack endermanItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta endermanMeta = endermanItem.getItemMeta();
        endermanMeta.setDisplayName("§d末影人掉落");
        endermanMeta.setLore(Arrays.asList(
            "§7当前状态: " + (endermanEnabled ? "§a✅ 开启" : "§c❌ 关闭"),
            "",
            "§e点击切换状态"
        ));
        endermanItem.setItemMeta(endermanMeta);
        
        menu.setItem(13, endermanItem);
        
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        
        for (int i = 0; i < 27; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, glass);
            }
        }
        
        player.openInventory(menu);
    }
}
