package com.vetsoftware.app.testtype.application.port.out;

import com.vetsoftware.app.testtype.domain.TestType;
import java.util.List;
import java.util.Optional;

public interface TestTypeRepository {
    TestType save(TestType testType);
    Optional<TestType> findById(Long id);
    List<TestType> findAll();
    void delete(Long id);
}
