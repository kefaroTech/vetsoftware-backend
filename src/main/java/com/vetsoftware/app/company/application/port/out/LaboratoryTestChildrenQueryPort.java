package com.vetsoftware.app.company.application.port.out;

public interface LaboratoryTestChildrenQueryPort {
  boolean existsActiveByCompanyId(Long parentId);
}
