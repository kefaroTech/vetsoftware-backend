package com.vetsoftware.app.testtype.application.usecase;

import com.vetsoftware.app.testtype.application.port.in.DeleteTestTypeUseCase;
import com.vetsoftware.app.testtype.application.port.out.TestTypeRepository;
import com.vetsoftware.app.testtype.domain.TestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "test_type.delete")
@Service
public class DeleteTestTypeService implements DeleteTestTypeUseCase {
    private final TestTypeRepository repository;

    public DeleteTestTypeService(TestTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new TestTypeNotFoundException(id));
        repository.delete(id);
    }
}
