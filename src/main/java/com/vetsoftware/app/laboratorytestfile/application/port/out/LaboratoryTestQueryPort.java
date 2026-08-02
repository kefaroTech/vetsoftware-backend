package com.vetsoftware.app.laboratorytestfile.application.port.out;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestStoragePathRef;
import java.util.Optional;

public interface LaboratoryTestQueryPort {
  Optional<LaboratoryTestRef> findById(Long laboratoryTestId);

  Optional<LaboratoryTestStoragePathRef> findStoragePath(Long laboratoryTestId);
}
