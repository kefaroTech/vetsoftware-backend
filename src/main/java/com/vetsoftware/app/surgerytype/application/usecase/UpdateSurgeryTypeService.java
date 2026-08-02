package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.command.UpdateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.UpdateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.CompanyRef;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery.type.update")
@Service
public class UpdateSurgeryTypeService implements UpdateSurgeryTypeUseCase {
    private final SurgeryTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateSurgeryTypeService(SurgeryTypeRepository repository,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public SurgeryTypeDto execute(UpdateSurgeryTypeCommand command) {
        SurgeryType surgeryType = repository.findById(command.id())
                .orElseThrow(() -> new SurgeryTypeNotFoundException(command.id()));
        CompanyRef company = command.companyId() == null
                ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Company not found: " + command.companyId()));
        surgeryType.update(command.name(), command.description(), company, command.general());
        return SurgeryTypeDto.from(repository.save(surgeryType));
    }
}
