package com.vetsoftware.app.clinicalhistory.application.port.out;

import com.vetsoftware.app.clinicalhistory.application.dto.AnimalReportInfo;
import java.util.Optional;

public interface AnimalReportQueryPort {
  Optional<AnimalReportInfo> findByIdAndCompanyId(Long animalId, Long companyId);
}
