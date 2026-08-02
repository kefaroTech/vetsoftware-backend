package com.vetsoftware.app.company.application.port.out;

public interface PermissionChildrenQueryPort {
  boolean existsActiveByCompanyId(Long parentId);
}
