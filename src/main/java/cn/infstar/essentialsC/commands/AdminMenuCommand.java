package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class AdminMenuCommand extends BaseCommand implements Listener {
    
    private static final int MENU_SIZE = 27;
    
    public AdminMenuCommand() {
        super("essentialsc.command.admin");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    protected boolean execute(@NotNull Player player, String[] args) {
        openMenu(player);
        return true;
    }
    
    private void openMenu(Player player) {
        String title = plugin.getConfig().getString("admin-menu.title", "&6EssentialsC 管理菜单");
        Inventory menu = Bukkit.createInventory(null, MENU_SIZE, translateColor(title));
        
        // 从配置中读取所有物品
        var itemsConfig = plugin.getConfig().getConfigurationSection("admin-menu.items");
        if (itemsConfig == null) return;
        
        for (String key : itemsConfig.getKeys(false)) {
            var section = itemsConfig.getConfigurationSection(key);
            if (section == null) continue;
            
            int slot = section.getInt("slot");
            Material material = Material.matchMaterial(section.getString("material", "STONE"));
            if (material == null) material = Material.STONE;
            
            String name = translateColor(section.getString("name", "&fItem"));
            java.util.List<String> lore = section.getStringList("lore").stream()
                .map(this::translateColor)
                .collect(java.util.stream.Collectors.toList());
            
            addItem(menu, slot, material, name, lore);
        }
        
        player.openInventory(menu);
    }
    
    private String translateColor(String text) {
        return text.replace("&", "§");
    }
    
    private void addItem(Inventory inv, int slot, Material material, String name, java.util.List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }
    
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        // 检查是否是管理员菜单
        String configTitle = translateColor(plugin.getConfig().getString("admin-menu.title", "&6EssentialsC 管理菜单"));
        if (!event.getView().getTitle().equals(configTitle)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // 取消事件，防止拿出物品
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String displayName = clicked.getItemMeta().getDisplayName();
        
        // 从配置中读取所有物品配置，通过名称匹配
        var itemsConfig = plugin.getConfig().getConfigurationSection("admin-menu.items");
        if (itemsConfig == null) return;
        
        for (String key : itemsConfig.getKeys(false)) {
            var section = itemsConfig.getConfigurationSection(key);
            if (section == null) continue;
            
            String itemName = translateColor(section.getString("name", ""));
            if (!displayName.equals(itemName)) continue;
            
            // 找到匹配的物品，执行对应操作
            switch (key) {
                case "time-control" -> {
                    if (event.isLeftClick()) player.getWorld().setTime(1000);
                    else player.getWorld().setTime(13000);
                    player.sendMessage(getLang().getString("admin-time-set"));
                }
                case "weather-control" -> {
                    if (event.isLeftClick()) player.getWorld().setStorm(false);
                    else player.getWorld().setStorm(true);
                    player.sendMessage(getLang().getString("admin-weather-set"));
                }
                case "heal-self" -> {
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    player.sendMessage(getLang().getString("admin-heal-success"));
                }
                case "feed-self" -> {
                    player.setFoodLevel(20);
                    player.setSaturation(20f);
                    player.sendMessage(getLang().getString("admin-feed-success"));
                }
                case "repair-hand" -> {
                    var item = player.getInventory().getItemInMainHand();
                    if (item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable d) {
                        d.setDamage(0);
                        item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) d);
                        player.sendMessage(getLang().getString("admin-repair-success"));
                    }
                }
                case "vanish" -> {
                    HelpCommand.COMMAND_CACHE.get("vanish").execute(player, new String[]{});
                    openMenu(player); // 刷新菜单
                }
                case "reload" -> {
                    plugin.reloadConfig();
                    EssentialsC.getLangManager().reload();
                    player.sendMessage(getLang().getString("admin-reload-success"));
                }
            }
            break; // 找到后退出循环
        }
    }
}
