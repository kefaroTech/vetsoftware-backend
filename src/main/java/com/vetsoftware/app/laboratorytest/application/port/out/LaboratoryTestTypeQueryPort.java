package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;
import java.util.Optional;

public interface LaboratoryTestTypeQueryPort {
  Optional<LaboratoryTestTypeRef> findById(Long testTypeId);
}
