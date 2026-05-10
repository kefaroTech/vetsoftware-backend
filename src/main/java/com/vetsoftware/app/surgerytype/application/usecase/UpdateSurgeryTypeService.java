package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.command.UpdateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.UpdateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery_type.update")
@Service
public class UpdateSurgeryTypeService implements UpdateSurgeryTypeUseCase {
    private final SurgeryTypeRepository repository;

    public UpdateSurgeryTypeService(SurgeryTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SurgeryTypeDto execute(UpdateSurgeryTypeCommand command) {
        SurgeryType surgeryType = repository.findById(command.id())
                .orElseThrow(() -> new SurgeryTypeNotFoundException(command.id()));
        surgeryType.update(command.name(), command.description());
        return SurgeryTypeDto.from(repository.save(surgeryType));
    }
}
