package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.command.CreateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.CreateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "surgery_type.create")
@Service
public class CreateSurgeryTypeService implements CreateSurgeryTypeUseCase {
    private final SurgeryTypeRepository repository;

    public CreateSurgeryTypeService(SurgeryTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public SurgeryTypeDto execute(CreateSurgeryTypeCommand command) {
        return SurgeryTypeDto.from(
                repository.save(SurgeryType.create(command.name(), command.description())));
    }
}
