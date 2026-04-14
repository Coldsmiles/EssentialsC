package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ShulkerBoxListener implements Listener {
    
    private final EssentialsC plugin;
    // 存储玩家打开的潜影盒：玩家UUID -> (原始物品快照, 当前物品引用)
    private final Map<UUID, ShulkerBoxData> openShulkerBoxes = new HashMap<>();
    
    // 预定义所有潜影盒材质（性能优化）
    private static final Set<Material> SHULKER_BOX_MATERIALS = Set.of(
        Material.SHULKER_BOX,
        Material.WHITE_SHULKER_BOX,
        Material.ORANGE_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX,
        Material.LIME_SHULKER_BOX,
        Material.PINK_SHULKER_BOX,
        Material.GRAY_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX,
        Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX,
        Material.GREEN_SHULKER_BOX,
        Material.RED_SHULKER_BOX,
        Material.BLACK_SHULKER_BOX
    );
    
    /**
     * 潜影盒数据记录
     */
    private static class ShulkerBoxData {
        ItemStack originalSnapshot; // 打开时的物品快照（用于验证）
        ItemStack currentItem;      // 当前物品引用（用于更新）
        int totalItems;             // 打开时的物品总数（用于防刷）
        
        ShulkerBoxData(ItemStack snapshot, ItemStack current, int items) {
            this.originalSnapshot = snapshot;
            this.currentItem = current;
            this.totalItems = items;
        }
    }
    
    public ShulkerBoxListener(EssentialsC plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只处理右键点击空气或方块的事件
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 检查权限
        if (!player.hasPermission("essentialsc.shulkerbox.open")) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null || !isShulkerBox(item)) {
            return;
        }
        
        // 只有潜行+右键才打开潜影盒
        if (!player.isSneaking()) {
            return;
        }
        
        // 取消默认行为（防止放置潜影盒）
        event.setCancelled(true);
        
        // 打开潜影盒
        openShulkerBox(player, item);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        ShulkerBoxData data = openShulkerBoxes.remove(playerId);
        
        if (data == null) {
            return;
        }
        
        Inventory closedInventory = event.getInventory();
        ItemStack currentItem = data.currentItem;
        
        // 验证物品是否还存在
        if (currentItem == null || currentItem.getType().isAir()) {
            // 物品已不存在，丢弃 inventory 中的所有物品
            for (ItemStack item : closedInventory.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            return;
        }
        
        // 更新潜影盒物品中的内容
        if (currentItem.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                // 将 inventory 的内容复制回潜影盒
                ItemStack[] contents = closedInventory.getContents();
                for (int i = 0; i < 27 && i < contents.length; i++) {
                    shulkerBox.getInventory().setItem(i, contents[i]);
                }
                
                // 更新物品元数据
                blockStateMeta.setBlockState(shulkerBox);
                currentItem.setItemMeta(blockStateMeta);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // 检查是否是玩家打开的潜影盒（使用 get 避免两次查找）
        if (openShulkerBoxes.get(player.getUniqueId()) == null) {
            return;
        }
        
        // 获取点击的物品
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) {
            return;
        }
        
        // 检查是否是潜影盒，如果是则阻止放置
        if (isShulkerBox(clickedItem)) {
            event.setCancelled(true);
            player.sendMessage("§c不能在潜影盒中放入另一个潜影盒！");
        }
    }
    

    
    /**
     * 检查物品是否为潜影盒（O(1) 时间复杂度）
     */
    private boolean isShulkerBox(ItemStack item) {
        return SHULKER_BOX_MATERIALS.contains(item.getType());
    }
    
    /**
     * 打开潜影盒
     */
    private void openShulkerBox(Player player, ItemStack shulkerBox) {
        // 获取潜影盒的 BlockStateMeta
        if (!(shulkerBox.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
            return;
        }
        
        // 获取潜影盒的方块状态
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBoxBlock)) {
            return;
        }
        
        // 创建物品快照（用于后续验证）
        ItemStack snapshot = shulkerBox.clone();
        
        // 计算当前物品总数（用于防刷检查）
        int totalItems = 0;
        for (ItemStack item : shulkerBoxBlock.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                totalItems += item.getAmount();
            }
        }
        
        // 获取潜影盒的自定义名称，如果没有则使用配置中的默认标题
        String title;
        if (shulkerBox.hasItemMeta() && shulkerBox.getItemMeta().hasDisplayName()) {
            // 使用潜影盒的自定义名称
            title = shulkerBox.getItemMeta().getDisplayName();
        } else {
            // 使用配置文件中的默认标题
            String defaultTitle = plugin.getConfig().getString("shulkerbox.default-title", "");
            if (defaultTitle != null && !defaultTitle.isEmpty()) {
                title = defaultTitle;
            } else {
                title = "&e潜影盒";
            }
        }
        
        // 如果 ProtocolLib 可用，标记需要修改标题
        InventoryTitleListener titleListener = EssentialsC.getInventoryTitleListener();
        if (titleListener != null) {
            titleListener.markForTitleChange(player, title);
        }
        
        // 创建一个新的 inventory（基于潜影盒的内容）
        // 注意：如果使用 ProtocolLib，标题会被拦截修改，这里用临时标题
        String inventoryTitle = titleListener != null ? "Opening..." : title.replace('&', '§');
        Inventory inventory = Bukkit.createInventory(null, 27, inventoryTitle);
        
        // 复制潜影盒的内容到新 inventory
        ItemStack[] contents = shulkerBoxBlock.getInventory().getContents();
        for (int i = 0; i < 27 && i < contents.length; i++) {
            inventory.setItem(i, contents[i]);
        }
        
        // 记录玩家打开的潜影盒（包含快照和当前引用）
        openShulkerBoxes.put(player.getUniqueId(), new ShulkerBoxData(snapshot, shulkerBox, totalItems));
        
        // 打开 inventory
        player.openInventory(inventory);
    }
}
