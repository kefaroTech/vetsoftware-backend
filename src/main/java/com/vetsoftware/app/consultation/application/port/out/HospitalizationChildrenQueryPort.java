package com.vetsoftware.app.consultation.application.port.out;

public interface HospitalizationChildrenQueryPort {
    boolean existsActiveByConsultationId(Long parentId);
}
