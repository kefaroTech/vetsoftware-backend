package com.vetsoftware.app.economicactivity.application.usecase;

import com.vetsoftware.app.economicactivity.application.port.in.DeleteEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "economic.activity.delete")
@Service
public class DeleteEconomicActivityService implements DeleteEconomicActivityUseCase {
  private final EconomicActivityRepository repository;

  public DeleteEconomicActivityService(EconomicActivityRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id) {
    repository.findById(id).orElseThrow(() -> new EconomicActivityNotFoundException(id));
    repository.delete(id);
  }
}
