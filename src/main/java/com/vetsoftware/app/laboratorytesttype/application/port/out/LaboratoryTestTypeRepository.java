package com.vetsoftware.app.laboratorytesttype.application.port.out;

import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import java.util.List;
import java.util.Optional;

public interface LaboratoryTestTypeRepository {
    LaboratoryTestType save(LaboratoryTestType laboratoryTestType);
    Optional<LaboratoryTestType> findById(Long id);
    List<LaboratoryTestType> findAll();
    List<LaboratoryTestType> findAllAvailableForCompany(Long companyId);
    void delete(Long id);
    int reactivate(Long id);
}
