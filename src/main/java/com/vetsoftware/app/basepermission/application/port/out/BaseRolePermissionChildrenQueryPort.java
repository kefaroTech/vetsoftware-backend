package com.vetsoftware.app.basepermission.application.port.out;

public interface BaseRolePermissionChildrenQueryPort {
    boolean existsActiveByBasePermissionId(Long parentId);
}
