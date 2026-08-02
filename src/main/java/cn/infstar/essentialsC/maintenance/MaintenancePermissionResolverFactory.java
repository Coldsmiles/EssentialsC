package cn.infstar.essentialsC.maintenance;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;

final class MaintenancePermissionResolverFactory {

    private static final String LUCKPERMS_RESOLVER_CLASS =
        "cn.infstar.essentialsC.maintenance.LuckPermsMaintenancePermissionResolver";

    private MaintenancePermissionResolverFactory() {
    }

    static MaintenancePermissionResolver create(EssentialsC plugin) {
        Plugin luckPerms = plugin.getServer().getPluginManager().getPlugin("LuckPerms");
        if (luckPerms == null || !luckPerms.isEnabled()) {
            plugin.getLogger().info("未检测到 LuckPerms，维护模式将在玩家加入后复核绕过权限。");
            return null;
        }

        try {
            Class<?> resolverClass = Class.forName(LUCKPERMS_RESOLVER_CLASS, true, plugin.getClass().getClassLoader());
            return (MaintenancePermissionResolver) resolverClass
                .getConstructor(EssentialsC.class)
                .newInstance(plugin);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                 | IllegalAccessException | InvocationTargetException | LinkageError exception) {
            plugin.getLogger().warning("LuckPerms 维护权限解析器初始化失败，将在玩家加入后复核权限: "
                + exception.getMessage());
            return null;
        }
    }
}
