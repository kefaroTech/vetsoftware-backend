package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.port.in.DeleteLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory_test_type.delete")
@Service
public class DeleteLaboratoryTestTypeService implements DeleteLaboratoryTestTypeUseCase {
    private final LaboratoryTestTypeRepository repository;

    public DeleteLaboratoryTestTypeService(LaboratoryTestTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new LaboratoryTestTypeNotFoundException(id));
        repository.delete(id);
    }
}
