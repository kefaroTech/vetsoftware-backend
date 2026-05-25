package com.vetsoftware.app.consultation.application.port.out;

import com.vetsoftware.app.consultation.domain.Consultation;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository {
    Consultation save(Consultation consultation);
    Optional<Consultation> findById(Long id);
    List<Consultation> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
