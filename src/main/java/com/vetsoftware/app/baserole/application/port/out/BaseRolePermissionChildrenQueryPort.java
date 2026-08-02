package com.vetsoftware.app.baserole.application.port.out;

public interface BaseRolePermissionChildrenQueryPort {
  boolean existsActiveByBaseRoleId(Long parentId);
}
