package com.vetsoftware.app.systemuser.application.port.out;

public interface SystemUserPermissionChildrenQueryPort {
    boolean existsActiveBySystemUserId(Long parentId);
}
