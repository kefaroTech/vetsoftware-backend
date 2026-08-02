package com.vetsoftware.app.role.application.port.out;

public interface EmployeeRoleChildrenQueryPort {
  boolean existsActiveByRoleId(Long parentId);
}
