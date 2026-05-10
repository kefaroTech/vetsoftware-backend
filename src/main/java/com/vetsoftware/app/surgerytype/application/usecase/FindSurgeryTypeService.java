package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.FindSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "surgery_type.find")
@Service
public class FindSurgeryTypeService implements FindSurgeryTypeUseCase {
    private final SurgeryTypeRepository repository;

    public FindSurgeryTypeService(SurgeryTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public SurgeryTypeDto findById(Long id) {
        return SurgeryTypeDto.from(repository.findById(id)
                .orElseThrow(() -> new SurgeryTypeNotFoundException(id)));
    }
}
