package com.vetsoftware.app.surgerytype.application.port.out;

import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import java.util.List;
import java.util.Optional;

public interface SurgeryTypeRepository {
    SurgeryType save(SurgeryType surgeryType);

    Optional<SurgeryType> findById(Long id);

    Optional<SurgeryType> findByIdAndCompanyId(Long id, Long companyId);

    List<SurgeryType> findAll();

    List<SurgeryType> findAllAvailableForCompany(Long companyId);

    void delete(Long id);

    int reactivate(Long id);
}
