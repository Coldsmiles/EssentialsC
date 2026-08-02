package cn.infstar.essentialsC.maintenance;

import java.util.UUID;

public interface MaintenancePermissionResolver {

    PermissionResult checkPermission(UUID uniqueId, String permission);

    enum PermissionResult {
        GRANTED,
        DENIED,
        UNAVAILABLE
    }
}
