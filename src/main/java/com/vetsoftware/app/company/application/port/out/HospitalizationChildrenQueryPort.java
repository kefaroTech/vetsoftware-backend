package com.vetsoftware.app.company.application.port.out;

public interface HospitalizationChildrenQueryPort {
  boolean existsActiveByCompanyId(Long parentId);
}
