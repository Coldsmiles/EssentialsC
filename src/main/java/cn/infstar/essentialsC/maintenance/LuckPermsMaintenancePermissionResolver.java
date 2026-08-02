package cn.infstar.essentialsC.maintenance;

import cn.infstar.essentialsC.EssentialsC;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LuckPermsMaintenancePermissionResolver implements MaintenancePermissionResolver {

    private final EssentialsC plugin;
    private final LuckPerms luckPerms;
    private final UserManager userManager;
    private final AtomicBoolean failureLogged = new AtomicBoolean();

    public LuckPermsMaintenancePermissionResolver(EssentialsC plugin) {
        this.plugin = plugin;
        this.luckPerms = plugin.getServer().getServicesManager().load(LuckPerms.class);
        if (luckPerms == null) {
            throw new IllegalStateException("LuckPerms 服务尚未注册");
        }
        this.userManager = luckPerms.getUserManager();
    }

    @Override
    public PermissionResult checkPermission(UUID uniqueId, String permission) {
        if (uniqueId == null || permission == null || permission.isBlank()) {
            return PermissionResult.DENIED;
        }

        try {
            User user = userManager.getUser(uniqueId);
            if (user == null) {
                user = userManager.loadUser(uniqueId).join();
            }

            QueryOptions queryOptions = luckPerms.getContextManager().getQueryOptions(user)
                .orElseGet(() -> luckPerms.getContextManager().getStaticQueryOptions());
            boolean granted = user.getCachedData()
                .getPermissionData(queryOptions)
                .checkPermission(permission)
                .asBoolean();
            failureLogged.set(false);
            return granted ? PermissionResult.GRANTED : PermissionResult.DENIED;
        } catch (RuntimeException exception) {
            if (failureLogged.compareAndSet(false, true)) {
                plugin.getLogger().warning("LuckPerms 维护权限查询失败，将在玩家加入后复核权限: "
                    + exception.getMessage());
            }
            return PermissionResult.UNAVAILABLE;
        }
    }
}
