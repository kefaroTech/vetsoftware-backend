package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import com.vetsoftware.app.laboratorytest.application.port.out.TestTypeQueryPort;
import com.vetsoftware.app.laboratorytest.domain.TestTypeRef;
import com.vetsoftware.app.testtype.infrastructure.persistence.TestTypeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("laboratoryTestJpaTestTypeQueryPort")
public class JpaTestTypeQueryPort implements TestTypeQueryPort {
    private final TestTypeJpaRepository testTypeJpaRepository;

    public JpaTestTypeQueryPort(TestTypeJpaRepository testTypeJpaRepository) {
        this.testTypeJpaRepository = testTypeJpaRepository;
    }

    @Override
    public Optional<TestTypeRef> findById(Long testTypeId) {
        return testTypeJpaRepository.findById(testTypeId)
            .map(e -> new TestTypeRef(e.getId(), e.getName()));
    }
}
