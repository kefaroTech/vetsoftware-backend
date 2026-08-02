package com.vetsoftware.app.state.application.usecase;

import com.vetsoftware.app.state.application.command.UpdateStateCommand;
import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.in.UpdateStateUseCase;
import com.vetsoftware.app.state.application.port.out.CountryQueryPort;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
import com.vetsoftware.app.state.domain.StateNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "state.update")
@Service
public class UpdateStateService implements UpdateStateUseCase {
  private final StateRepository repository;
  private final CountryQueryPort countryQueryPort;

  public UpdateStateService(StateRepository repository, CountryQueryPort countryQueryPort) {
    this.repository = repository;
    this.countryQueryPort = countryQueryPort;
  }

  @Override
  @Transactional
  public StateDto execute(UpdateStateCommand command) {
    State state =
        repository
            .findById(command.id())
            .orElseThrow(() -> new StateNotFoundException(command.id()));
    CountryRef country =
        countryQueryPort
            .findById(command.countryId())
            .orElseThrow(
                () -> new IllegalArgumentException("Country not found: " + command.countryId()));
    state.update(command.name(), country, command.daneCode());
    return StateDto.from(repository.save(state));
  }
}
