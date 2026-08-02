package com.vetsoftware.app.company.application.port.out;

public interface ConsultationChildrenQueryPort {
  boolean existsActiveByCompanyId(Long parentId);
}
