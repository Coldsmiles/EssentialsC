package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ShulkerBoxListener implements Listener {

    private static final int SHULKER_SIZE = 27;

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
    private final Map<UUID, OpenShulkerSession> openShulkerBoxes = new HashMap<>();

    private static final class ShulkerBoxHolder implements InventoryHolder {
        private final Inventory inventory;

        private ShulkerBoxHolder(Component title) {
            this.inventory = Bukkit.createInventory(this, SHULKER_SIZE, title);
        }

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }
    }

    private static final class OpenShulkerSession {
        private final ItemStack sourceItem;
        private final EquipmentSlot sourceHand;
        private final int preferredSlot;

        private OpenShulkerSession(ItemStack sourceItem, EquipmentSlot sourceHand, int preferredSlot) {
            this.sourceItem = sourceItem;
            this.sourceHand = sourceHand;
            this.preferredSlot = preferredSlot;
        }
    }

    public ShulkerBoxListener(EssentialsC plugin) {
        this.plugin = plugin;
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

        if (openShulkerBoxes.containsKey(player.getUniqueId())) {
            return;
        }

        EquipmentSlot hand = event.getHand() == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        ItemStack sourceItem = getItemFromHand(player, hand);
        if (!isShulkerBox(sourceItem)) {
            return;
        }
        if (sourceItem.getAmount() != 1) {
            player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.shulkerbox-unstack-first"));
            return;
        }

        if (!(sourceItem.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
            return;
        }
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            return;
        }

        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

        ItemStack sourceSnapshot = sourceItem.clone();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || openShulkerBoxes.containsKey(player.getUniqueId())) {
                return;
            }

            ItemStack currentItem = getItemFromHand(player, hand);
            if (!isSameShulkerItem(currentItem, sourceSnapshot)) {
                return;
            }

            openShulkerBox(player, hand, sourceSnapshot, shulkerBox);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();
        if (!(topInventory.getHolder(false) instanceof ShulkerBoxHolder)) {
            return;
        }

        int topSize = topInventory.getSize();
        boolean clickTopInventory = event.getRawSlot() >= 0 && event.getRawSlot() < topSize;

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

        if (clickTopInventory && event.getClick() == ClickType.SWAP_OFFHAND) {
            ItemStack offHandItem = player.getInventory().getItemInOffHand();
            if (isShulkerBox(offHandItem)) {
                event.setCancelled(true);
                sendNestedMessage(player);
                return;
            }
        }

        if (event.isShiftClick() && isShulkerBox(event.getCurrentItem())) {
            event.setCancelled(true);
            sendNestedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();
        if (!(topInventory.getHolder(false) instanceof ShulkerBoxHolder)) {
            return;
        }

        if (!isShulkerBox(event.getOldCursor())) {
            return;
        }

        int topSize = topInventory.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < topSize) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    sendNestedMessage(player);
                }
                return;
            }
        }
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        commitOpenShulker(player, player.getOpenInventory().getTopInventory());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        commitOpenShulker(player, player.getOpenInventory().getTopInventory());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            commitOpenShulker(player, player.getOpenInventory().getTopInventory());
        }
    }

    private boolean commitOpenShulker(Player player, Inventory inventory) {
        if (!(inventory.getHolder(false) instanceof ShulkerBoxHolder)) {
            return false;
        }

        OpenShulkerSession session = openShulkerBoxes.remove(player.getUniqueId());
        if (session == null) {
            return false;
        }

        ItemStack updatedShulker = session.sourceItem.clone();
        writeInventoryBack(updatedShulker, inventory.getContents());
        restoreItemToPlayer(player, session, updatedShulker);
        return true;
    }

    private void openShulkerBox(Player player, EquipmentSlot hand, ItemStack sourceItem, ShulkerBox shulkerBox) {
        removeItemFromHand(player, hand);

        ShulkerBoxHolder holder = new ShulkerBoxHolder(resolveTitle(sourceItem));
        holder.getInventory().setContents(cloneContents(shulkerBox.getInventory().getContents()));

        int preferredSlot = hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : -1;
        openShulkerBoxes.put(player.getUniqueId(), new OpenShulkerSession(sourceItem, hand, preferredSlot));
        player.openInventory(holder.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 0.8F, 1.0F);
    }

    private void writeInventoryBack(ItemStack shulkerItem, ItemStack[] contents) {
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
            plugin.getLogger().warning("Failed to save shulker box contents: missing BlockStateMeta.");
            return;
        }

        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            plugin.getLogger().warning("Failed to save shulker box contents: block state is not a ShulkerBox.");
            return;
        }

        shulkerBox.getInventory().setContents(cloneContents(contents));
        blockStateMeta.setBlockState(shulkerBox);
        shulkerItem.setItemMeta(blockStateMeta);
    }

    private void restoreItemToPlayer(Player player, OpenShulkerSession session, ItemStack shulkerItem) {
        PlayerInventory inventory = player.getInventory();

        if (session.sourceHand == EquipmentSlot.OFF_HAND) {
            if (isEmpty(inventory.getItemInOffHand())) {
                inventory.setItemInOffHand(shulkerItem);
                return;
            }
        } else if (session.preferredSlot >= 0 && isEmpty(inventory.getItem(session.preferredSlot))) {
            inventory.setItem(session.preferredSlot, shulkerItem);
            return;
        }

        Map<Integer, ItemStack> leftovers = inventory.addItem(shulkerItem);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] copied = new ItemStack[SHULKER_SIZE];
        for (int i = 0; i < SHULKER_SIZE && i < contents.length; i++) {
            copied[i] = contents[i] == null ? null : contents[i].clone();
        }
        return copied;
    }

    private ItemStack getItemFromHand(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND
            ? player.getInventory().getItemInOffHand()
            : player.getInventory().getItemInMainHand();
    }

    private void removeItemFromHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
        } else {
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), null);
        }
    }

    private Component resolveTitle(ItemStack shulkerBox) {
        ItemMeta itemMeta = shulkerBox.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            Component displayName = itemMeta.displayName();
            if (displayName != null) {
                return displayName;
            }
            return LegacyComponentSerializer.legacySection().deserialize(itemMeta.getDisplayName());
        }

        return Component.translatable(shulkerBox.getType().getItemTranslationKey());
    }

    private void sendNestedMessage(Player player) {
        player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.shulkerbox-nested"));
    }

    private boolean isShulkerBox(ItemStack item) {
        return item != null && !item.getType().isAir() && SHULKER_BOX_MATERIALS.contains(item.getType());
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private boolean isSameShulkerItem(ItemStack currentItem, ItemStack sourceSnapshot) {
        if (!isShulkerBox(currentItem) || sourceSnapshot == null) {
            return false;
        }
        return currentItem.getAmount() == sourceSnapshot.getAmount() && currentItem.isSimilar(sourceSnapshot);
    }
}
