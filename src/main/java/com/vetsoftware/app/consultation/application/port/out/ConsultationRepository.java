package com.vetsoftware.app.consultation.application.port.out;

import com.vetsoftware.app.consultation.domain.Consultation;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository {
    Consultation save(Consultation consultation);
    Optional<Consultation> findById(Long id);
    Optional<Consultation> findByIdAndCompanyId(Long id, Long companyId);
    List<Consultation> findAll();
    List<Consultation> findAllByCompanyId(Long companyId);
    void delete(Long id, Long companyId);
    int reactivate(Long id, Long companyId);
}
