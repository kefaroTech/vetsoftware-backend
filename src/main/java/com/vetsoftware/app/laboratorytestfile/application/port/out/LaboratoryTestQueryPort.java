package com.vetsoftware.app.laboratorytestfile.application.port.out;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import java.util.Optional;

public interface LaboratoryTestQueryPort {
    Optional<LaboratoryTestRef> findById(Long laboratoryTestId);
}
