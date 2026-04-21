package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * 生物掉落物控制命令
 * /mobdrops - 打开控制菜单
 */
public class MobDropCommand extends BaseCommand {

    public static final class MobDropMenuHolder implements InventoryHolder {
        private final Inventory inventory;

        public MobDropMenuHolder(String title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
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
    
    /**
     * 打开生物掉落控制菜单
     */
    private void openMobDropMenu(Player player) {
        // 读取当前配置
        boolean endermanEnabled = plugin.getConfig().getBoolean("mob-drops.enderman.enabled", true);
        
        // 创建菜单
        Inventory menu = new MobDropMenuHolder("§6§l生物掉落控制").getInventory();
        
        // 末影人控制项
        ItemStack endermanItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta endermanMeta = endermanItem.getItemMeta();
        endermanMeta.setDisplayName("§d末影人掉落");
        endermanMeta.setLore(Arrays.asList(
            "§7当前状态: " + (endermanEnabled ? "§a✅ 开启" : "§c❌ 关闭"),
            "",
            "§e点击切换状态"
        ));
        endermanItem.setItemMeta(endermanMeta);
        
        // 放置在中间
        menu.setItem(13, endermanItem);
        
        // 装饰物品
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
