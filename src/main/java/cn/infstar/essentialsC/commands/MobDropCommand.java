package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Map;

public class MobDropCommand extends BaseCommand {

    private static final int MENU_SIZE = 27;
    private static final int ENDERMAN_SLOT = 13;

    public static final class MobDropMenuHolder implements InventoryHolder {
        private final Inventory inventory;

        public MobDropMenuHolder(String title) {
            this.inventory = Bukkit.createInventory(this, MENU_SIZE, title);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public MobDropCommand() {
        super("essentialsc.mobdrops.enderman");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        openMobDropMenu(player);
        return true;
    }

    public static void openMobDropMenu(EssentialsC plugin, Player player) {
        var lang = EssentialsC.getLangManager();
        boolean endermanDropsAllowed = plugin.getConfig().getBoolean("mob-drops.enderman.allow-drops", true);
        String status = lang.getString(endermanDropsAllowed
            ? "mobdrops-menu.status.enabled"
            : "mobdrops-menu.status.disabled");

        Inventory menu = new MobDropMenuHolder(lang.getString("mobdrops-menu.title")).getInventory();

        ItemStack endermanItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta endermanMeta = endermanItem.getItemMeta();
        if (endermanMeta != null) {
            endermanMeta.displayName(legacyComponent(lang.getString("mobdrops-menu.enderman.name")));
            endermanMeta.lore(List.of(
                lang.getString("mobdrops-menu.enderman.status", Map.of("status", status)),
                "",
                lang.getString("mobdrops-menu.enderman.toggle")
            ).stream().map(MobDropCommand::legacyComponent).toList());
            endermanItem.setItemMeta(endermanMeta);
        }
        menu.setItem(ENDERMAN_SLOT, endermanItem);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.text(" "));
            glass.setItemMeta(glassMeta);
        }

        for (int slot = 0; slot < MENU_SIZE; slot++) {
            if (menu.getItem(slot) == null) {
                menu.setItem(slot, glass);
            }
        }

        player.openInventory(menu);
    }

    public static int getEndermanSlot() {
        return ENDERMAN_SLOT;
    }

    private static Component legacyComponent(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text == null ? "" : text);
    }

    private void openMobDropMenu(Player player) {
        openMobDropMenu(plugin, player);
    }
}
