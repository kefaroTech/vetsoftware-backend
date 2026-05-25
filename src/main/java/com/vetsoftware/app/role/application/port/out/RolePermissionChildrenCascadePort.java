package com.vetsoftware.app.role.application.port.out;

public interface RolePermissionChildrenCascadePort {
    int deactivateAllByRoleId(Long roleId);
}
