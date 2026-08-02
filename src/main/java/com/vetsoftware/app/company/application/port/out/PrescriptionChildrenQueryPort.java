package com.vetsoftware.app.company.application.port.out;

public interface PrescriptionChildrenQueryPort {
  boolean existsActiveByCompanyId(Long parentId);
}
