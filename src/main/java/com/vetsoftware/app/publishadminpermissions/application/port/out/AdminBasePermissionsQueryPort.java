package com.vetsoftware.app.publishadminpermissions.application.port.out;

import java.util.List;

public interface AdminBasePermissionsQueryPort {
    List<AdminBasePermission> findByAdminBaseRoleId(Long adminBaseRoleId);
}
