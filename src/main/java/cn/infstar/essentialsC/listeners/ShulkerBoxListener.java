package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class ShulkerBoxListener implements Listener {

    private static final int SHULKER_SIZE = 27;
    private static final int OFF_HAND_SLOT = 40;

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

    private final EssentialsC plugin;
    private final NamespacedKey sessionKey;
    private final Map<UUID, OpenShulkerSession> openShulkerBoxes = new HashMap<>();

    private static final class ShulkerBoxHolder implements InventoryHolder {
        private final Inventory inventory;

        private ShulkerBoxHolder(Component title) {
            this.inventory = Bukkit.createInventory(this, InventoryType.SHULKER_BOX, title);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class OpenShulkerSession {
        private final String token;
        private final Inventory inventory;
        private final int preferredSlot;
        private boolean syncScheduled;

        private OpenShulkerSession(String token, Inventory inventory, int preferredSlot) {
            this.token = token;
            this.inventory = inventory;
            this.preferredSlot = preferredSlot;
        }
    }

    private record LocatedSource(ItemStack item, Consumer<ItemStack> save) {
    }

    public ShulkerBoxListener(EssentialsC plugin) {
        this.plugin = plugin;
        this.sessionKey = new NamespacedKey(plugin, "open_shulker_session");
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearStaleSessionTokens(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking() || !player.hasPermission("essentialsc.shulkerbox.open")) {
            return;
        }

        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) {
            return;
        }

        if (openShulkerBoxes.containsKey(player.getUniqueId())) {
            return;
        }

        EquipmentSlot hand = event.getHand() == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        ItemStack sourceItem = getItemFromHand(player, hand);
        if (!isShulkerBox(sourceItem)) {
            return;
        }

        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

        if (sourceItem.getAmount() != 1) {
            player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.shulkerbox-unstack-first"));
            return;
        }

        ItemStack sourceSnapshot = sourceItem.clone();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || openShulkerBoxes.containsKey(player.getUniqueId())) {
                return;
            }

            ItemStack currentItem = getItemFromHand(player, hand);
            if (!isSameShulkerItem(currentItem, sourceSnapshot)) {
                return;
            }

            openShulkerBox(player, hand, currentItem);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        OpenShulkerSession session = openShulkerBoxes.get(player.getUniqueId());
        if (session == null || event.getView().getTopInventory() != session.inventory) {
            return;
        }

        if (hasSessionToken(event.getCurrentItem(), session.token)
            || hasSessionToken(event.getCursor(), session.token)) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (hasSessionToken(hotbarItem, session.token)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND
            && hasSessionToken(player.getInventory().getItemInOffHand(), session.token)) {
            event.setCancelled(true);
            return;
        }

        int topSize = session.inventory.getSize();
        boolean clickTopInventory = ShulkerBoxSessionPolicy.isTopSlot(event.getRawSlot(), topSize);

        if (clickTopInventory && isShulkerBox(event.getCursor())) {
            event.setCancelled(true);
            sendNestedMessage(player);
            return;
        }

        if (clickTopInventory && event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isShulkerBox(hotbarItem)) {
                event.setCancelled(true);
                sendNestedMessage(player);
                return;
            }
        }

        if (clickTopInventory && event.getClick() == ClickType.SWAP_OFFHAND
            && isShulkerBox(player.getInventory().getItemInOffHand())) {
            event.setCancelled(true);
            sendNestedMessage(player);
            return;
        }

        if (event.isShiftClick() && isShulkerBox(event.getCurrentItem())) {
            event.setCancelled(true);
            sendNestedMessage(player);
            return;
        }

        scheduleSynchronization(player, session);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        OpenShulkerSession session = openShulkerBoxes.get(player.getUniqueId());
        if (session == null || event.getView().getTopInventory() != session.inventory) {
            return;
        }

        if (hasSessionToken(event.getOldCursor(), session.token)) {
            event.setCancelled(true);
            return;
        }

        if (isShulkerBox(event.getOldCursor())
            && ShulkerBoxSessionPolicy.touchesTopInventory(event.getRawSlots(), session.inventory.getSize())) {
            event.setCancelled(true);
            sendNestedMessage(player);
            return;
        }

        scheduleSynchronization(player, session);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (commitOpenShulker(player, event.getInventory())) {
            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 0.8F, 1.0F);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        OpenShulkerSession session = openShulkerBoxes.get(event.getPlayer().getUniqueId());
        if (session != null && hasSessionToken(event.getItemDrop().getItemStack(), session.token)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        OpenShulkerSession session = openShulkerBoxes.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }

        if (hasSessionToken(event.getMainHandItem(), session.token)
            || hasSessionToken(event.getOffHandItem(), session.token)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        OpenShulkerSession session = openShulkerBoxes.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        boolean saved;
        if (event.getKeepInventory()) {
            saved = writeSessionToPlayer(player, session, true);
        } else {
            saved = writeSessionToDrops(event.getDrops(), session);
            if (!saved) {
                saved = writeSessionToPlayer(player, session, true);
            }
        }

        if (!saved) {
            handleLostSource(player, session, "玩家死亡时未找到唯一的源物品");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        commitCurrentSession(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        commitCurrentSession(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!openShulkerBoxes.containsKey(event.getPlayer().getUniqueId())) {
            clearStaleSessionTokens(event.getPlayer());
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            shutdown();
        }
    }

    public void shutdown() {
        for (UUID playerId : List.copyOf(openShulkerBoxes.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                openShulkerBoxes.remove(playerId);
                plugin.getLogger().warning("潜影盒会话关闭失败: 玩家 " + playerId + " 已离线。");
                continue;
            }

            OpenShulkerSession session = openShulkerBoxes.get(playerId);
            boolean viewingSession = session != null && player.getOpenInventory().getTopInventory() == session.inventory;
            commitCurrentSession(player);
            if (viewingSession) {
                player.closeInventory();
            }
        }
    }

    private void openShulkerBox(Player player, EquipmentSlot hand, ItemStack sourceItem) {
        if (!(sourceItem.getItemMeta() instanceof BlockStateMeta blockStateMeta)
            || !(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            sendOpenFailedMessage(player);
            return;
        }

        String token = UUID.randomUUID().toString();
        ItemStack taggedSource = sourceItem.clone();
        setSessionToken(taggedSource, token);
        setItemInHand(player, hand, taggedSource);

        ShulkerBoxHolder holder;
        try {
            holder = new ShulkerBoxHolder(resolveTitle(sourceItem));
            holder.getInventory().setContents(cloneContents(shulkerBox.getInventory().getContents()));
        } catch (RuntimeException exception) {
            clearTokenFromPlayer(player, token);
            plugin.getLogger().warning("打开潜影盒失败: " + exception.getMessage());
            sendOpenFailedMessage(player);
            return;
        }

        int preferredSlot = hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : OFF_HAND_SLOT;
        OpenShulkerSession session = new OpenShulkerSession(token, holder.getInventory(), preferredSlot);
        openShulkerBoxes.put(player.getUniqueId(), session);

        try {
            InventoryView openedView = player.openInventory(session.inventory);
            if (openedView == null
                || openedView.getTopInventory() != session.inventory
                || player.getOpenInventory().getTopInventory() != session.inventory) {
                rollbackFailedOpen(player, session);
                return;
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("打开潜影盒失败: " + exception.getMessage());
            rollbackFailedOpen(player, session);
            return;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 0.8F, 1.0F);
    }

    private void rollbackFailedOpen(Player player, OpenShulkerSession session) {
        openShulkerBoxes.remove(player.getUniqueId(), session);
        clearTokenFromPlayer(player, session.token);
        sendOpenFailedMessage(player);
    }

    private void scheduleSynchronization(Player player, OpenShulkerSession session) {
        if (session.syncScheduled) {
            return;
        }

        session.syncScheduled = true;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            session.syncScheduled = false;
            if (openShulkerBoxes.get(player.getUniqueId()) != session) {
                return;
            }

            if (!writeSessionToPlayer(player, session, false)) {
                openShulkerBoxes.remove(player.getUniqueId(), session);
                handleLostSource(player, session, "同步期间未找到唯一的源物品");
                if (player.getOpenInventory().getTopInventory() == session.inventory) {
                    player.closeInventory();
                }
            }
        });
    }

    private void commitCurrentSession(Player player) {
        OpenShulkerSession session = openShulkerBoxes.get(player.getUniqueId());
        if (session != null) {
            commitOpenShulker(player, session.inventory);
        }
    }

    private boolean commitOpenShulker(Player player, Inventory inventory) {
        OpenShulkerSession session = openShulkerBoxes.get(player.getUniqueId());
        if (session == null || session.inventory != inventory) {
            return false;
        }

        openShulkerBoxes.remove(player.getUniqueId(), session);
        boolean saved = writeSessionToPlayer(player, session, true);
        if (!saved) {
            handleLostSource(player, session, "关闭期间未找到唯一的源物品");
        }
        return saved;
    }

    private boolean writeSessionToPlayer(Player player, OpenShulkerSession session, boolean clearToken) {
        List<LocatedSource> sources = findSources(player, session);
        if (sources.size() != 1) {
            clearSessionTokens(sources);
            return false;
        }

        LocatedSource source = sources.getFirst();
        if (source.item().getAmount() != 1) {
            clearSessionTokens(sources);
            return false;
        }

        ItemStack updatedItem = source.item().clone();
        if (!writeInventoryBack(updatedItem, session.inventory.getContents(), clearToken)) {
            clearSessionTokens(sources);
            return false;
        }

        source.save().accept(updatedItem);
        return true;
    }

    private boolean writeSessionToDrops(List<ItemStack> drops, OpenShulkerSession session) {
        List<Integer> matchingIndexes = new ArrayList<>();
        for (int index = 0; index < drops.size(); index++) {
            if (hasSessionToken(drops.get(index), session.token)) {
                matchingIndexes.add(index);
            }
        }

        if (matchingIndexes.size() != 1) {
            clearTokensFromDrops(drops, matchingIndexes);
            return false;
        }

        int sourceIndex = matchingIndexes.getFirst();
        ItemStack sourceItem = drops.get(sourceIndex);
        if (sourceItem.getAmount() != 1) {
            clearTokensFromDrops(drops, matchingIndexes);
            return false;
        }

        ItemStack updatedItem = sourceItem.clone();
        if (!writeInventoryBack(updatedItem, session.inventory.getContents(), true)) {
            clearTokensFromDrops(drops, matchingIndexes);
            return false;
        }

        drops.set(sourceIndex, updatedItem);
        return true;
    }

    private boolean writeInventoryBack(ItemStack shulkerItem, ItemStack[] contents, boolean clearToken) {
        if (containsShulkerBox(contents)) {
            plugin.getLogger().warning("保存潜影盒内容失败: 虚拟容器中检测到嵌套潜影盒。");
            return false;
        }

        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
            plugin.getLogger().warning("保存潜影盒内容失败: 缺少 BlockStateMeta。");
            return false;
        }

        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            plugin.getLogger().warning("保存潜影盒内容失败: 方块状态不是 ShulkerBox。");
            return false;
        }

        try {
            shulkerBox.getInventory().setContents(cloneContents(contents));
            blockStateMeta.setBlockState(shulkerBox);
            if (clearToken) {
                blockStateMeta.getPersistentDataContainer().remove(sessionKey);
            }
            shulkerItem.setItemMeta(blockStateMeta);
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("保存潜影盒内容失败: " + exception.getMessage());
            return false;
        }
    }

    private List<LocatedSource> findSources(Player player, OpenShulkerSession session) {
        List<LocatedSource> sources = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();

        if (session.preferredSlot >= 0 && session.preferredSlot < inventory.getSize()) {
            addSourceIfMatching(
                sources,
                inventory.getItem(session.preferredSlot),
                item -> inventory.setItem(session.preferredSlot, item),
                session.token
            );
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot == session.preferredSlot) {
                continue;
            }
            int inventorySlot = slot;
            addSourceIfMatching(
                sources,
                inventory.getItem(inventorySlot),
                item -> inventory.setItem(inventorySlot, item),
                session.token
            );
        }

        addSourceIfMatching(sources, player.getItemOnCursor(), player::setItemOnCursor, session.token);
        return sources;
    }

    private void addSourceIfMatching(
        List<LocatedSource> sources,
        ItemStack item,
        Consumer<ItemStack> save,
        String token
    ) {
        if (hasSessionToken(item, token)) {
            sources.add(new LocatedSource(item, save));
        }
    }

    private void clearSessionTokens(List<LocatedSource> sources) {
        for (LocatedSource source : sources) {
            ItemStack cleanedItem = source.item().clone();
            clearSessionToken(cleanedItem);
            source.save().accept(cleanedItem);
        }
    }

    private void clearTokensFromDrops(List<ItemStack> drops, List<Integer> indexes) {
        for (int index : indexes) {
            ItemStack cleanedItem = drops.get(index).clone();
            clearSessionToken(cleanedItem);
            drops.set(index, cleanedItem);
        }
    }

    private void clearTokenFromPlayer(Player player, String token) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!hasSessionToken(item, token)) {
                continue;
            }
            ItemStack cleanedItem = item.clone();
            clearSessionToken(cleanedItem);
            inventory.setItem(slot, cleanedItem);
        }

        ItemStack cursor = player.getItemOnCursor();
        if (hasSessionToken(cursor, token)) {
            ItemStack cleanedCursor = cursor.clone();
            clearSessionToken(cleanedCursor);
            player.setItemOnCursor(cleanedCursor);
        }
    }

    private void clearStaleSessionTokens(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!hasAnySessionToken(item)) {
                continue;
            }
            ItemStack cleanedItem = item.clone();
            clearSessionToken(cleanedItem);
            inventory.setItem(slot, cleanedItem);
        }

        ItemStack cursor = player.getItemOnCursor();
        if (hasAnySessionToken(cursor)) {
            ItemStack cleanedCursor = cursor.clone();
            clearSessionToken(cleanedCursor);
            player.setItemOnCursor(cleanedCursor);
        }
    }

    private void handleLostSource(Player player, OpenShulkerSession session, String reason) {
        clearTokenFromPlayer(player, session.token);
        plugin.getLogger().warning("潜影盒会话已终止: " + player.getName() + " (" + player.getUniqueId() + ")，" + reason + "。");
        if (player.isOnline()) {
            player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.shulkerbox-session-invalid"));
        }
    }

    private void setSessionToken(ItemStack item, String token) {
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.getPersistentDataContainer().set(sessionKey, PersistentDataType.STRING, token);
        item.setItemMeta(itemMeta);
    }

    private void clearSessionToken(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        itemMeta.getPersistentDataContainer().remove(sessionKey);
        item.setItemMeta(itemMeta);
    }

    private boolean hasSessionToken(ItemStack item, String token) {
        if (!isShulkerBox(item)) {
            return false;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return false;
        }
        String storedToken = itemMeta.getPersistentDataContainer().get(sessionKey, PersistentDataType.STRING);
        return token.equals(storedToken);
    }

    private boolean hasAnySessionToken(ItemStack item) {
        if (!isShulkerBox(item)) {
            return false;
        }
        ItemMeta itemMeta = item.getItemMeta();
        return itemMeta != null && itemMeta.getPersistentDataContainer().has(sessionKey, PersistentDataType.STRING);
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] copied = new ItemStack[SHULKER_SIZE];
        for (int index = 0; index < SHULKER_SIZE && index < contents.length; index++) {
            copied[index] = contents[index] == null ? null : contents[index].clone();
        }
        return copied;
    }

    private boolean containsShulkerBox(ItemStack[] contents) {
        for (ItemStack item : contents) {
            if (isShulkerBox(item)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack getItemFromHand(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND
            ? player.getInventory().getItemInOffHand()
            : player.getInventory().getItemInMainHand();
    }

    private void setItemInHand(Player player, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), item);
        }
    }

    private Component resolveTitle(ItemStack shulkerBox) {
        ItemMeta itemMeta = shulkerBox.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            Component displayName = itemMeta.displayName();
            if (displayName != null) {
                return displayName;
            }
        }

        return Component.translatable(shulkerBox.getType().getItemTranslationKey());
    }

    private void sendNestedMessage(Player player) {
        player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.shulkerbox-nested"));
    }

    private void sendOpenFailedMessage(Player player) {
        player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.shulkerbox-open-failed"));
    }

    private boolean isShulkerBox(ItemStack item) {
        return item != null && !item.getType().isAir() && SHULKER_BOX_MATERIALS.contains(item.getType());
    }

    private boolean isSameShulkerItem(ItemStack currentItem, ItemStack sourceSnapshot) {
        if (!isShulkerBox(currentItem) || sourceSnapshot == null) {
            return false;
        }
        return currentItem.getAmount() == sourceSnapshot.getAmount() && currentItem.isSimilar(sourceSnapshot);
    }
}
