package com.vetsoftware.app.economicactivity.application.usecase;

import com.vetsoftware.app.economicactivity.application.command.CreateEconomicActivityCommand;
import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.in.CreateEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "economic.activity.create")
@Service
public class CreateEconomicActivityService implements CreateEconomicActivityUseCase {
  private final EconomicActivityRepository repository;

  public CreateEconomicActivityService(EconomicActivityRepository repository) {
    this.repository = repository;
  }

  @Override
  public EconomicActivityDto execute(CreateEconomicActivityCommand command) {
    if (repository.existsByCode(command.code())) {
      throw new IllegalArgumentException("EconomicActivity code already exists: " + command.code());
    }
    return EconomicActivityDto.from(
        repository.save(EconomicActivity.create(command.code(), command.name())));
  }
}
