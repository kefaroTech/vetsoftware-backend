package com.vetsoftware.app.systempermission.application.port.out;

public interface SystemUserPermissionChildrenQueryPort {
    boolean existsActiveBySystemPermissionId(Long parentId);
}
