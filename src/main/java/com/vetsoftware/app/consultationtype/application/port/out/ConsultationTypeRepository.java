package com.vetsoftware.app.consultationtype.application.port.out;

import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import java.util.List;
import java.util.Optional;

public interface ConsultationTypeRepository {
    ConsultationType save(ConsultationType consultationType);
    Optional<ConsultationType> findById(Long id);
    List<ConsultationType> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
