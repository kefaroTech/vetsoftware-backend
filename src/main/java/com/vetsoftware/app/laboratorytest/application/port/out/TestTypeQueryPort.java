package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.domain.TestTypeRef;
import java.util.Optional;

public interface TestTypeQueryPort {
    Optional<TestTypeRef> findById(Long testTypeId);
}
