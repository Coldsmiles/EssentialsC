package cn.infstar.essentialsC.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BlocksMenuCommand extends BaseCommand implements Listener {

    private static final int MENU_SIZE = 36;
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
        String title = plugin.getConfig().getString("blocks-menu.title", "&6&lEssentialsC &8- &e&l鍔熻兘鏂瑰潡鑿滃崟");
        Inventory menu = new BlocksMenuHolder(translateColor(title)).getInventory();

        var itemsConfig = plugin.getConfig().getConfigurationSection("blocks-menu.items");
        if (itemsConfig == null) {
            return;
        }

        for (String key : itemsConfig.getKeys(false)) {
            var section = itemsConfig.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String permission = section.getString("permission");
            if (permission != null && !player.hasPermission(permission)) {
                continue;
            }

            int slot = section.getInt("slot");
            Material material = Material.matchMaterial(section.getString("material", "STONE"));
            if (material == null) {
                material = Material.STONE;
            }

            String name = translateColor(section.getString("name", "&fItem"));
            java.util.List<String> lore = section.getStringList("lore").stream()
                .map(this::translateColor)
                .toList();

            addItem(menu, slot, material, name, lore, key);
        }

        player.openInventory(menu);
    }

    private void addItem(Inventory inv, int slot, Material material, String name, java.util.List<String> lore, String key) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(this.blockKey, PersistentDataType.STRING, key);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof BlocksMenuHolder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        String key = meta.getPersistentDataContainer().get(this.blockKey, PersistentDataType.STRING);
        if (key != null && HelpCommand.COMMAND_CACHE.containsKey(key)) {
            playBlockOpenSound(player, key);
            HelpCommand.COMMAND_CACHE.get(key).execute(player, new String[0]);
        }
    }

    private void playBlockOpenSound(Player player, String key) {
        org.bukkit.Sound sound = switch (key) {
            case "workbench" -> org.bukkit.Sound.BLOCK_WOOD_HIT;
            case "anvil" -> org.bukkit.Sound.BLOCK_ANVIL_USE;
            case "cartographytable" -> org.bukkit.Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT;
            case "grindstone" -> org.bukkit.Sound.BLOCK_GRINDSTONE_USE;
            case "loom" -> org.bukkit.Sound.UI_LOOM_TAKE_RESULT;
            case "smithingtable" -> org.bukkit.Sound.BLOCK_SMITHING_TABLE_USE;
            case "stonecutter" -> org.bukkit.Sound.BLOCK_STONE_HIT;
            case "enderchest" -> org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN;
            default -> null;
        };

        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    private String translateColor(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }
}
