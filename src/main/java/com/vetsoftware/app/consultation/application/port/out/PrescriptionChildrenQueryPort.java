package com.vetsoftware.app.consultation.application.port.out;

public interface PrescriptionChildrenQueryPort {
  boolean existsActiveByConsultationId(Long parentId);
}
