package com.vetsoftware.app.surgerytype.application.port.out;

import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import java.util.List;
import java.util.Optional;

public interface SurgeryTypeRepository {
    SurgeryType save(SurgeryType surgeryType);
    Optional<SurgeryType> findById(Long id);
    List<SurgeryType> findAll();
    void delete(Long id);
}
