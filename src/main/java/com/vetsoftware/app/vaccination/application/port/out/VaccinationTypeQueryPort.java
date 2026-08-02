package com.vetsoftware.app.vaccination.application.port.out;

import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;
import java.util.Optional;

public interface VaccinationTypeQueryPort {
  Optional<VaccinationTypeRef> findById(Long vaccinationTypeId);
}
