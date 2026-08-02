package com.vetsoftware.app.animalalert.application.usecase;

import com.vetsoftware.app.animalalert.application.command.UpdateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import com.vetsoftware.app.animalalert.application.port.in.UpdateAnimalAlertUseCase;
import com.vetsoftware.app.animalalert.application.port.out.AnimalAlertRepository;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.animalalert.domain.AnimalAlertNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.alert.update")
@Service
public class UpdateAnimalAlertService implements UpdateAnimalAlertUseCase {
    private final AnimalAlertRepository repository;

    public UpdateAnimalAlertService(AnimalAlertRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AnimalAlertDto execute(UpdateAnimalAlertCommand command) {
        AnimalAlert alert = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new AnimalAlertNotFoundException(command.id()));
        alert.update(command.type(), command.description(), command.severity());
        return AnimalAlertDto.from(repository.save(alert));
    }
}
