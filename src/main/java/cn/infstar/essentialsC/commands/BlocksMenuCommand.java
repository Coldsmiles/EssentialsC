package cn.infstar.essentialsC.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlocksMenuCommand extends BaseCommand implements Listener {

    private static final int MENU_SIZE = 36;
    private static final int[] DIVIDER_SLOTS = {4, 13, 22, 31};
    private static boolean listenerRegistered = false;

    private final NamespacedKey blockKey;

    private static final class BlocksMenuHolder implements InventoryHolder {
        private final Inventory inventory;

        private BlocksMenuHolder(String title) {
            this.inventory = Bukkit.createInventory(this, MENU_SIZE, title);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public BlocksMenuCommand() {
        super("essentialsc.command.blocks");
        addConfigDefaults();
        if (!listenerRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
        }
        this.blockKey = new NamespacedKey(plugin, "block_key");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        Inventory menu = new BlocksMenuHolder(getLang().getString("blocks-menu.title")).getInventory();

        var sectionsConfig = plugin.getConfig().getConfigurationSection("blocks-menu.sections");
        if (sectionsConfig != null) {
            int visibleSections = renderSections(menu, player, sectionsConfig);
            if (visibleSections > 1) {
                renderDivider(menu);
            }
            if (menu.isEmpty()) {
                player.sendMessage(getLang().getPrefixedString("messages.blocks-menu-empty"));
                return;
            }
            player.openInventory(menu);
            return;
        }

        var itemsConfig = plugin.getConfig().getConfigurationSection("blocks-menu.items");
        if (itemsConfig == null) {
            return;
        }

        renderItems(menu, player, itemsConfig);
        if (menu.isEmpty()) {
            player.sendMessage(getLang().getPrefixedString("messages.blocks-menu-empty"));
            return;
        }

        player.openInventory(menu);
    }

    private int renderSections(Inventory menu, Player player, org.bukkit.configuration.ConfigurationSection sectionsConfig) {
        int visibleSections = 0;
        for (String sectionKey : sectionsConfig.getKeys(false)) {
            var section = sectionsConfig.getConfigurationSection(sectionKey);
            if (section == null) {
                continue;
            }

            var itemsConfig = section.getConfigurationSection("items");
            if (itemsConfig == null) {
                continue;
            }

            List<MenuItem> visibleItems = collectVisibleItems(player, itemsConfig);
            if (visibleItems.isEmpty()) {
                continue;
            }

            visibleSections++;
            for (MenuItem item : visibleItems) {
                addItem(menu, item);
            }
        }
        return visibleSections;
    }

    private void renderItems(Inventory menu, Player player, org.bukkit.configuration.ConfigurationSection itemsConfig) {
        for (MenuItem item : collectVisibleItems(player, itemsConfig)) {
            addItem(menu, item);
        }
    }

    private List<MenuItem> collectVisibleItems(Player player, org.bukkit.configuration.ConfigurationSection itemsConfig) {
        List<MenuItem> items = new ArrayList<>();
        for (String key : itemsConfig.getKeys(false)) {
            var section = itemsConfig.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String permission = section.getString("permission");
            if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
                continue;
            }

            String commandKey = section.getString("command", key);
            if (!CommandRegistry.isAvailable(commandKey)) {
                continue;
            }

            MenuItem item = createMenuItem(section, commandKey);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private MenuItem createMenuItem(org.bukkit.configuration.ConfigurationSection section, String commandKey) {
        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= MENU_SIZE) {
            return null;
        }

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }

        String name = getLang().getString("blocks-menu.items." + commandKey + ".name");
        List<String> lore = getLang().getStringList("blocks-menu.items." + commandKey + ".lore");
        return new MenuItem(slot, material, name, lore, commandKey);
    }

    private void addItem(Inventory inventory, MenuItem menuItem) {
        ItemStack item = new ItemStack(menuItem.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(menuItem.name());
            meta.setLore(menuItem.lore().isEmpty() ? null : menuItem.lore());
            if (menuItem.commandKey() != null && !menuItem.commandKey().isBlank()) {
                meta.getPersistentDataContainer().set(blockKey, PersistentDataType.STRING, menuItem.commandKey());
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(menuItem.slot(), item);
    }

    private void renderDivider(Inventory inventory) {
        ItemStack divider = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = divider.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            divider.setItemMeta(meta);
        }

        for (int slot : DIVIDER_SLOTS) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, divider);
            }
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof BlocksMenuHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        String key = meta.getPersistentDataContainer().get(blockKey, PersistentDataType.STRING);
        if (key == null || key.isBlank()) {
            return;
        }

        BaseCommand blockCommand = CommandRegistry.getCommand(key);
        if (blockCommand == null) {
            return;
        }

        String permission = blockCommand.getPermission();
        if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
            player.sendMessage(getLang().getPrefixedString("messages.no-permission",
                Map.of("permission", permission)));
            return;
        }

        blockCommand.execute(player, new String[0]);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof BlocksMenuHolder)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private String translateColor(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    private void addConfigDefaults() {
        plugin.getConfig().addDefault("blocks-menu.layout-version", 2);

        addMenuItemDefaults("blocks-menu.sections.blocks.items.workbench", 10, "CRAFTING_TABLE",
            "essentialsc.command.workbench", "workbench");
        addMenuItemDefaults("blocks-menu.sections.blocks.items.enderchest", 11, "ENDER_CHEST",
            "essentialsc.command.enderchest", "enderchest");
        addMenuItemDefaults("blocks-menu.sections.blocks.items.anvil", 12, "ANVIL",
            "essentialsc.command.anvil", "anvil");
        addMenuItemDefaults("blocks-menu.sections.blocks.items.grindstone", 19, "GRINDSTONE",
            "essentialsc.command.grindstone", "grindstone");
        addMenuItemDefaults("blocks-menu.sections.blocks.items.smithingtable", 20, "SMITHING_TABLE",
            "essentialsc.command.smithingtable", "smithingtable");
        addMenuItemDefaults("blocks-menu.sections.blocks.items.stonecutter", 21, "STONECUTTER",
            "essentialsc.command.stonecutter", "stonecutter");
        addMenuItemDefaults("blocks-menu.sections.blocks.items.loom", 28, "LOOM",
            "essentialsc.command.loom", "loom");
        addMenuItemDefaults("blocks-menu.sections.blocks.items.cartographytable", 29, "CARTOGRAPHY_TABLE",
            "essentialsc.command.cartographytable", "cartographytable");

        addMenuItemDefaults("blocks-menu.sections.shortcuts.items.nightvision", 14, "TINTED_GLASS",
            "essentialsc.command.nightvision", "nightvision");
        addMenuItemDefaults("blocks-menu.sections.shortcuts.items.glow", 15, "GLOWSTONE",
            "essentialsc.command.glow", "glow");

        plugin.getConfig().options().copyDefaults(true);
        migrateLayoutIfNeeded();
        plugin.saveConfig();
    }

    private void migrateLayoutIfNeeded() {
        boolean hasStoredLayoutVersion = plugin.getConfig().contains("blocks-menu.layout-version", true);
        if (hasStoredLayoutVersion && plugin.getConfig().getInt("blocks-menu.layout-version", 0) >= 2) {
            return;
        }

        applySlot("blocks", "workbench", 10);
        applySlot("blocks", "enderchest", 11);
        applySlot("blocks", "anvil", 12);
        applySlot("blocks", "grindstone", 19);
        applySlot("blocks", "smithingtable", 20);
        applySlot("blocks", "stonecutter", 21);
        applySlot("blocks", "loom", 28);
        applySlot("blocks", "cartographytable", 29);
        applySlot("shortcuts", "nightvision", 14);
        applySlot("shortcuts", "glow", 15);

        plugin.getConfig().set("blocks-menu.sections.blocks.title-item", null);
        plugin.getConfig().set("blocks-menu.sections.shortcuts.title-item", null);
        plugin.getConfig().set("blocks-menu.layout-version", 2);
    }

    private void applySlot(String section, String key, int slot) {
        plugin.getConfig().set("blocks-menu.sections." + section + ".items." + key + ".slot", slot);
    }

    private void addMenuItemDefaults(String path, int slot, String material, String permission, String command) {
        plugin.getConfig().addDefault(path + ".slot", slot);
        plugin.getConfig().addDefault(path + ".material", material);
        plugin.getConfig().addDefault(path + ".permission", permission);
        plugin.getConfig().addDefault(path + ".command", command);
    }

    private record MenuItem(int slot, Material material, String name, List<String> lore, String commandKey) {
    }
}
