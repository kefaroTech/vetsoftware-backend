package com.vetsoftware.app.consultation.application.port.out;

public interface LaboratoryTestChildrenQueryPort {
  boolean existsActiveByConsultationId(Long parentId);
}
