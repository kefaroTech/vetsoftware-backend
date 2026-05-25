package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.in.ReactivateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratorytest.reactivate")
@Service
public class ReactivateLaboratoryTestService implements ReactivateLaboratoryTestUseCase {
    private final LaboratoryTestRepository repository;

    public ReactivateLaboratoryTestService(LaboratoryTestRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public LaboratoryTestDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new LaboratoryTestNotFoundException(id);
        return LaboratoryTestDto.from(repository.findById(id)
            .orElseThrow(() -> new LaboratoryTestNotFoundException(id)));
    }
}
