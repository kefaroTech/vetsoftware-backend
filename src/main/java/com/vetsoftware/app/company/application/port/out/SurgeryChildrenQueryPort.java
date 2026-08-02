package com.vetsoftware.app.company.application.port.out;

public interface SurgeryChildrenQueryPort {
  boolean existsActiveByCompanyId(Long parentId);
}
