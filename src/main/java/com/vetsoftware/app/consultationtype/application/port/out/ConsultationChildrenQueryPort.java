package com.vetsoftware.app.consultationtype.application.port.out;

public interface ConsultationChildrenQueryPort {
  boolean existsActiveByConsultationTypeId(Long parentId);
}
