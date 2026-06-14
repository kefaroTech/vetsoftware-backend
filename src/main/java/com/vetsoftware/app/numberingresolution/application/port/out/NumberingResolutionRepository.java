package com.vetsoftware.app.numberingresolution.application.port.out;

import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import java.util.List;
import java.util.Optional;

public interface NumberingResolutionRepository {
    NumberingResolution save(NumberingResolution resolution);
    Optional<NumberingResolution> findById(Long id);
    List<NumberingResolution> findAllByCompanyId(Long companyId);
    void delete(Long id);
    int reactivate(Long id);
}
