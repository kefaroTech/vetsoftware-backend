package com.vetsoftware.app.economicactivity.application.usecase;

import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.in.FindEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "economic.activity.find")
@Service
public class FindEconomicActivityService implements FindEconomicActivityUseCase {
  private final EconomicActivityRepository repository;

  public FindEconomicActivityService(EconomicActivityRepository repository) {
    this.repository = repository;
  }

  @Override
  public EconomicActivityDto findById(Long id) {
    return EconomicActivityDto.from(
        repository.findById(id).orElseThrow(() -> new EconomicActivityNotFoundException(id)));
  }
}
