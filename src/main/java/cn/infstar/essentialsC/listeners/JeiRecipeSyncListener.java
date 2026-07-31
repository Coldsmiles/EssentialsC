package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.compat.jei.JeiRecipeSyncAdapter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 为 Fabric / NeoForge 客户端补发配方同步数据，修复 1.21.2+ 的 JEI 配方显示问题。
 */
public class JeiRecipeSyncListener implements Listener {

    private static final Set<String> WARNED_UNSUPPORTED_VERSIONS = ConcurrentHashMap.newKeySet();

    private final EssentialsC plugin;
    private final boolean debug;
    private final boolean sendPlayerMessage;
    private final int brandCheckDelayTicks;
    private final JeiRecipeSyncAdapter adapter;

    public JeiRecipeSyncListener(EssentialsC plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfig();
        config.addDefault("jei-sync.brand-check-delay-ticks", 20);
        config.options().copyDefaults(true);

        this.debug = config.getBoolean("debug", false);
        this.sendPlayerMessage = config.getBoolean("jei-sync.send-player-message", true);
        this.brandCheckDelayTicks = Math.max(0, config.getInt("jei-sync.brand-check-delay-ticks", 20));
        this.adapter = loadAdapter();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (adapter == null) {
            if (debug) {
                plugin.getLogger().warning("当前服务端版本没有可用的 JEI 配方同步适配器。");
            }
            return;
        }

        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> detectAndSync(player), brandCheckDelayTicks);
    }

    private void detectAndSync(Player player) {
        if (!player.isOnline()) {
            return;
        }

        String clientBrand = player.getClientBrandName();

        if (debug) {
            plugin.getLogger().info("JEI 客户端检测: player=" + player.getName()
                + ", brand=" + (clientBrand == null || clientBrand.isBlank() ? "unknown" : clientBrand)
                + ", delayTicks=" + brandCheckDelayTicks);
        }

        if (clientBrand == null || clientBrand.isEmpty()) {
            if (debug) {
                plugin.getLogger().info("跳过 " + player.getName() + "：客户端品牌为空");
            }
            return;
        }

        String brandLower = clientBrand.toLowerCase(Locale.ROOT);
        if (brandLower.contains("fabric")) {
            if (debug) {
                plugin.getLogger().info("检测到 Fabric 客户端，开始发送配方同步...");
            }
            sendPlayerMessage(player, "Fabric");
            sendFabricRecipeSync(player);
            return;
        }

        if (brandLower.contains("neoforge") || brandLower.contains("forge")) {
            if (debug) {
                plugin.getLogger().info("检测到 NeoForge/Forge 客户端，开始发送配方同步...");
            }
            sendPlayerMessage(player, "NeoForge");
            sendNeoForgeRecipeSync(player);
            return;
        }

        if (debug) {
            plugin.getLogger().info("跳过 " + player.getName() + "：不支持的客户端类型 '" + clientBrand + "'");
        }
    }

    private void sendPlayerMessage(Player player, String clientType) {
        if (!sendPlayerMessage) {
            return;
        }

        String messageKey;
        if (clientType.equalsIgnoreCase("fabric")) {
            messageKey = "messages.jei-sync-fabric";
        } else if (clientType.equalsIgnoreCase("neoforge")) {
            messageKey = "messages.jei-sync-neoforge";
        } else {
            return;
        }

        player.sendMessage(EssentialsC.getLangManager().getPrefixedComponent(messageKey));
    }

    private void sendFabricRecipeSync(Player player) {
        try {
            adapter.sendFabricRecipeSync(player, plugin.getLogger(), debug);
        } catch (Exception e) {
            plugin.getLogger().warning("发送 Fabric 配方同步失败: " + e.getMessage());
            if (debug) {
                plugin.getLogger().log(Level.WARNING, "Fabric 配方同步异常详情", e);
            }
        }
    }

    private void sendNeoForgeRecipeSync(Player player) {
        try {
            adapter.sendNeoForgeRecipeSync(player, plugin.getLogger(), debug);
        } catch (Exception e) {
            plugin.getLogger().warning("发送 NeoForge 配方同步失败: " + e.getMessage());
            if (debug) {
                plugin.getLogger().log(Level.WARNING, "NeoForge 配方同步异常详情", e);
            }
        }
    }

    private JeiRecipeSyncAdapter loadAdapter() {
        String versionKey = plugin.getServer().getBukkitVersion().split("-")[0]
            .replace('.', '_');
        String className = "cn.infstar.essentialsC.v" + versionKey + ".jei.JeiRecipeSyncAdapterImpl";
        try {
            Class<?> adapterClass = Class.forName(className);
            Object instance = adapterClass.getDeclaredConstructor().newInstance();
            if (instance instanceof JeiRecipeSyncAdapter jeiRecipeSyncAdapter) {
                if (debug) {
                    plugin.getLogger().info("已加载 JEI 配方同步适配器: " + className);
                }
                return jeiRecipeSyncAdapter;
            }
            plugin.getLogger().warning("JEI 配方同步适配器类型无效: " + className);
        } catch (ReflectiveOperationException | LinkageError e) {
            if (WARNED_UNSUPPORTED_VERSIONS.add(versionKey)) {
                plugin.getLogger().warning("当前 Paper 版本 " + plugin.getServer().getBukkitVersion()
                    + " 没有可用的 JEI 配方同步适配器；仅 JEI 同步功能已停用。");
            }
            if (debug) {
                plugin.getLogger().warning("加载 JEI 配方同步适配器失败: " + className + " (" + e.getMessage() + ")");
            }
        }
        return null;
    }
}
