package com.vetsoftware.app.consultation.application.port.out;

public interface SurgeryChildrenQueryPort {
    boolean existsActiveByConsultationId(Long parentId);
}
