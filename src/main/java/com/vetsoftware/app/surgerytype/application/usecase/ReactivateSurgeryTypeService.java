package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.ReactivateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgerytype.reactivate")
@Service
public class ReactivateSurgeryTypeService implements ReactivateSurgeryTypeUseCase {
    private final SurgeryTypeRepository repository;

    public ReactivateSurgeryTypeService(SurgeryTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SurgeryTypeDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new SurgeryTypeNotFoundException(id);
        return SurgeryTypeDto.from(repository.findById(id)
            .orElseThrow(() -> new SurgeryTypeNotFoundException(id)));
    }
}
