package com.vetsoftware.app.consultation.application.port.out;

public interface DewormingChildrenQueryPort {
  boolean existsActiveByConsultationId(Long parentId);
}
