package com.vetsoftware.app.consultation.application.port.out;

public interface DiagnosticImagingChildrenQueryPort {
  boolean existsActiveByConsultationId(Long parentId);
}
