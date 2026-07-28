package com.vetsoftware.app.economicactivity.application.usecase;

import com.vetsoftware.app.economicactivity.application.command.UpdateEconomicActivityCommand;
import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.in.UpdateEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "economic.activity.update")
@Service
public class UpdateEconomicActivityService implements UpdateEconomicActivityUseCase {
    private final EconomicActivityRepository repository;

    public UpdateEconomicActivityService(EconomicActivityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EconomicActivityDto execute(UpdateEconomicActivityCommand command) {
        EconomicActivity economicActivity = repository.findById(command.id())
                .orElseThrow(() -> new EconomicActivityNotFoundException(command.id()));
        economicActivity.update(command.code(), command.name());
        return EconomicActivityDto.from(repository.save(economicActivity));
    }
}
