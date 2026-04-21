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
import org.jetbrains.annotations.NotNull;

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
     * 潜影盒 Inventory Holder - 用于识别自定义 inventory
     */
    private static class ShulkerBoxHolder implements org.bukkit.inventory.InventoryHolder {
        private final Inventory inventory;
        private final ItemStack shulkerBoxItem;
        private final ItemStack[] currentContents; // 实时追踪的物品内容
        
        ShulkerBoxHolder(ItemStack shulkerBoxItem, String title) {
            this.shulkerBoxItem = shulkerBoxItem;
            this.inventory = Bukkit.createInventory(this, 27, title);
            this.currentContents = new ItemStack[27];
        }
        
        @Override
        public @NotNull Inventory getInventory() {
            return this.inventory;
        }
        
        public ItemStack getShulkerBoxItem() {
            return shulkerBoxItem;
        }
        
        /**
         * 更新指定槽位的物品（供外部调用）
         */
        public void updateSlot(int slot, ItemStack item) {
            if (slot >= 0 && slot < 27) {
                currentContents[slot] = item;
            }
        }
        
        /**
         * 获取当前追踪的所有物品内容
         */
        public ItemStack[] getCurrentContents() {
            return currentContents.clone();
        }
    }
    
    /**
     * 潜影盒数据记录
     */
    private static class ShulkerBoxData {
        int slotIndex;              // 玩家背包中的槽位索引
        ItemStack originalSnapshot; // 打开时的物品快照（用于验证）
        int totalItems;             // 打开时的物品总数（用于防刷）
        
        ShulkerBoxData(int slot, ItemStack snapshot, int items) {
            this.slotIndex = slot;
            this.originalSnapshot = snapshot;
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
        
        // 查找物品在玩家背包中的槽位
        int slotIndex = -1;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem != null && invItem.isSimilar(item)) {
                slotIndex = i;
                break;
            }
        }
        
        // 打开潜影盒（传入槽位索引）
        openShulkerBox(player, item, slotIndex);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        Inventory closedInventory = event.getInventory();
        
        // 检查是否是潜影盒 inventory
        if (!(closedInventory.getHolder(false) instanceof ShulkerBoxHolder holder)) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        ShulkerBoxData data = openShulkerBoxes.remove(playerId);
        
        if (data == null) {
            return;
        }
        
        plugin.getLogger().info("=== 潜影盒关闭（数据已在点击时实时保存） ===");
    }
    
    @EventHandler
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        Inventory openedInventory = event.getInventory();
        if (openedInventory.getHolder(false) instanceof ShulkerBoxHolder) {
            plugin.getLogger().info("[Open] ✅ 潜影盒 inventory 已打开");
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onInventoryClickDebug(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        
        if (clickedInventory.getHolder(false) instanceof ShulkerBoxHolder) {
            plugin.getLogger().info("[Click-LOWEST] 检测到点击事件 | 槽位: " + event.getSlot() + 
                " | 物品: " + (event.getCurrentItem() != null ? event.getCurrentItem().getType() : "null"));
        }
    }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        
        // 检查是否是潜影盒 inventory
        if (!(clickedInventory.getHolder(false) instanceof ShulkerBoxHolder holder)) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        ShulkerBoxData data = openShulkerBoxes.get(playerId);
        if (data == null) {
            return;
        }
        
        // 阻止嵌套潜影盒
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && isShulkerBox(clickedItem)) {
            event.setCancelled(true);
            player.sendMessage(EssentialsC.getLangManager().getString("prefix") + 
                EssentialsC.getLangManager().getString("messages.shulkerbox-nested"));
            return;
        }
        
        // ✅ CMILib 方式：延迟 1 tick 后立即保存到潜影盒 NBT
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("[Click] === 开始处理点击事件 ===");
            
            // 读取当前 inventory 内容
            ItemStack[] contents = clickedInventory.getContents();
            
            // 调试
            int nonEmpty = 0;
            StringBuilder items = new StringBuilder();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && !contents[i].getType().isAir()) {
                    nonEmpty++;
                    items.append(String.format("\n  [%d] %s x%d", i, contents[i].getType(), contents[i].getAmount()));
                }
            }
            plugin.getLogger().info("[Click] 非空槽位: " + nonEmpty);
            plugin.getLogger().info("[Click] 物品详情:" + items);
            
            // 获取玩家背包中的潜影盒物品
            ItemStack shulkerItem = null;
            if (data.slotIndex >= 0 && data.slotIndex < player.getInventory().getSize()) {
                shulkerItem = player.getInventory().getItem(data.slotIndex);
                plugin.getLogger().info("[Click] 槽位 " + data.slotIndex + " 物品: " + 
                    (shulkerItem != null ? shulkerItem.getType() : "null"));
            }
            
            if (shulkerItem == null || shulkerItem.getType().isAir()) {
                plugin.getLogger().warning("[Click] ❌ 潜影盒物品不存在");
                return;
            }
            
            // 更新潜影盒的 BlockState
            if (shulkerItem.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                if (blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                    // 设置 inventory 内容
                    for (int i = 0; i < 27 && i < contents.length; i++) {
                        shulkerBox.getInventory().setItem(i, contents[i]);
                    }
                    
                    // 更新元数据
                    blockStateMeta.setBlockState(shulkerBox);
                    shulkerItem.setItemMeta(blockStateMeta);
                    
                    // 写回玩家背包
                    player.getInventory().setItem(data.slotIndex, shulkerItem);
                    
                    plugin.getLogger().info("[Click] ✅ 已实时保存 " + nonEmpty + " 个物品槽");
                } else {
                    plugin.getLogger().warning("[Click] ❌ BlockState 不是 ShulkerBox");
                }
            } else {
                plugin.getLogger().warning("[Click] ❌ 没有 BlockStateMeta");
            }
        });
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
    private void openShulkerBox(Player player, ItemStack shulkerBox, int slotIndex) {
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
                // 转换颜色代码 & -> §
                title = defaultTitle.replace('&', '§');
            } else {
                // 如果配置为空，使用 "Shulker Box"（客户端会自动翻译）
                title = "Shulker Box";
            }
        }
        
        // 创建 ShulkerBoxHolder（会自动创建 inventory）
        ShulkerBoxHolder holder = new ShulkerBoxHolder(shulkerBox, title);
        Inventory inventory = holder.getInventory();
        
        // 复制潜影盒的内容到 inventory
        ItemStack[] contents = shulkerBoxBlock.getInventory().getContents();
        for (int i = 0; i < 27 && i < contents.length; i++) {
            inventory.setItem(i, contents[i]);
        }
        
        // 记录玩家打开的潜影盒（保存槽位索引和快照）
        openShulkerBoxes.put(player.getUniqueId(), new ShulkerBoxData(slotIndex, snapshot, totalItems));
        
        // 打开 inventory
        player.openInventory(inventory);
    }
}
