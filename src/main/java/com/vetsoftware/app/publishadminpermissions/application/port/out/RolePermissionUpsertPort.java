package com.vetsoftware.app.publishadminpermissions.application.port.out;

public interface RolePermissionUpsertPort {
  boolean linkIfAbsent(Long roleId, Long permissionId);
}
