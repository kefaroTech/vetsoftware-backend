package com.vetsoftware.app.consultation.application.port.out;

public interface VaccinationChildrenQueryPort {
  boolean existsActiveByConsultationId(Long parentId);
}
