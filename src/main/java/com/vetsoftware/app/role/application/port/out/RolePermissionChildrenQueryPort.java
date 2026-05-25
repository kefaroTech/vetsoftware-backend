package com.vetsoftware.app.role.application.port.out;

public interface RolePermissionChildrenQueryPort {
    boolean existsActiveByRoleId(Long parentId);
}
