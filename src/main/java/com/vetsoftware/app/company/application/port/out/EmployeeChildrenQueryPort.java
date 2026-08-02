package com.vetsoftware.app.company.application.port.out;

public interface EmployeeChildrenQueryPort {
  boolean existsActiveByCompanyId(Long parentId);
}
