package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.command.ChangeSpaStatusCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.in.ChangeSpaStatusUseCase;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import com.vetsoftware.app.spa.domain.SpaStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa.change.status")
@Service
public class ChangeSpaStatusService implements ChangeSpaStatusUseCase {
    private final SpaRepository repository;

    public ChangeSpaStatusService(SpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SpaDto execute(ChangeSpaStatusCommand command) {
        Spa spa = (command.companyId() == null
            ? repository.findById(command.id())
            : repository.findByIdAndCompanyId(command.id(), command.companyId()))
            .orElseThrow(() -> new SpaNotFoundException(command.id()));
        SpaStatus newStatus = SpaStatus.valueOf(command.status().toUpperCase());
        spa.changeStatus(newStatus);
        return SpaDto.from(repository.save(spa));
    }
}
