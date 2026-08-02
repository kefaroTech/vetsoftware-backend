package com.vetsoftware.app.permission.application.port.out;

public interface RolePermissionChildrenQueryPort {
  boolean existsActiveByPermissionId(Long parentId);
}
