package com.vetsoftware.app.publishadminpermissions.application.port.out;

public interface PermissionUpsertPort {
    UpsertedPermission upsert(Long companyId, AdminBasePermission template);
}
