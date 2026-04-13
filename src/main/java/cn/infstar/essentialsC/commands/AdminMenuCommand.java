package cn.infstar.essentialsC.commands;

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
        String title = getLang().getString("admin-menu-title");
        Inventory menu = Bukkit.createInventory(null, MENU_SIZE, title);
        
        // 时间控制
        addItem(menu, 10, Material.CLOCK, getLang().getString("admin-time-control"), 
                Arrays.asList("§7左键: 设为白天", "§7右键: 设为夜晚"));
        addItem(menu, 11, Material.SUNFLOWER, getLang().getString("admin-weather-control"), 
                Arrays.asList("§7左键: 晴天", "§7右键: 雨天"));
        
        // 状态恢复
        addItem(menu, 13, Material.GOLDEN_APPLE, getLang().getString("admin-heal-self"), 
                Arrays.asList("§7补满生命值和饱食度"));
        addItem(menu, 14, Material.BREAD, getLang().getString("admin-feed-self"), 
                Arrays.asList("§7补满饱食度"));
        addItem(menu, 15, Material.ANVIL, getLang().getString("admin-repair-hand"), 
                Arrays.asList("§7修复当前手持物品"));
        
        // 管理员功能
        addItem(menu, 21, Material.ENDER_PEARL, getLang().getString("admin-vanish"), 
                Arrays.asList("§7点击切换隐身状态"));
        addItem(menu, 22, Material.BOOK, getLang().getString("admin-reload"), 
                Arrays.asList("§7重新加载配置文件"));
        
        player.openInventory(menu);
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
        String title = getLang().getString("admin-menu-title");
        if (!event.getView().getTitle().equals(title)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String name = clicked.getItemMeta().getDisplayName();
        String timeControl = getLang().getString("admin-time-control");
        String weatherControl = getLang().getString("admin-weather-control");
        String healSelf = getLang().getString("admin-heal-self");
        String feedSelf = getLang().getString("admin-feed-self");
        String repairHand = getLang().getString("admin-repair-hand");
        String vanish = getLang().getString("admin-vanish");
        String reload = getLang().getString("admin-reload");
        
        switch (name) {
            case String t when t.equals(timeControl) -> {
                if (event.isLeftClick()) player.getWorld().setTime(1000);
                else player.getWorld().setTime(13000);
                player.sendMessage(getLang().getString("admin-time-set"));
            }
            case String w when w.equals(weatherControl) -> {
                if (event.isLeftClick()) player.getWorld().setStorm(false);
                else player.getWorld().setStorm(true);
                player.sendMessage(getLang().getString("admin-weather-set"));
            }
            case String h when h.equals(healSelf) -> {
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.sendMessage(getLang().getString("admin-heal-success"));
            }
            case String f when f.equals(feedSelf) -> {
                player.setFoodLevel(20);
                player.setSaturation(20f);
                player.sendMessage(getLang().getString("admin-feed-success"));
            }
            case String r when r.equals(repairHand) -> {
                var item = player.getInventory().getItemInMainHand();
                if (item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable d) {
                    d.setDamage(0);
                    item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) d);
                    player.sendMessage(getLang().getString("admin-repair-success"));
                }
            }
            case String v when v.equals(vanish) -> {
                new VanishCommand().execute(player, new String[]{});
                openMenu(player); // 刷新菜单
            }
            case String rl when rl.equals(reload) -> {
                plugin.reloadConfig();
                player.sendMessage(getLang().getString("admin-reload-success"));
            }
            default -> {} // 忽略其他点击
        }
    }
}
