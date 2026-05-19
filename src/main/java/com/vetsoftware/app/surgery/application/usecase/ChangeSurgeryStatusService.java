package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.command.ChangeSurgeryStatusCommand;
import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.in.ChangeSurgeryStatusUseCase;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgery.domain.SurgeryStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery.change_status")
@Service
public class ChangeSurgeryStatusService implements ChangeSurgeryStatusUseCase {
    private final SurgeryRepository repository;

    public ChangeSurgeryStatusService(SurgeryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SurgeryDto execute(ChangeSurgeryStatusCommand command) {
        Surgery surgery = repository.findById(command.id())
            .orElseThrow(() -> new SurgeryNotFoundException(command.id()));
        SurgeryStatus newStatus = SurgeryStatus.valueOf(command.status().toUpperCase());
        surgery.changeStatus(newStatus);
        return SurgeryDto.from(repository.save(surgery));
    }
}
