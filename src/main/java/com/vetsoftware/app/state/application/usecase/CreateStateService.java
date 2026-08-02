package com.vetsoftware.app.state.application.usecase;

import com.vetsoftware.app.state.application.command.CreateStateCommand;
import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.in.CreateStateUseCase;
import com.vetsoftware.app.state.application.port.out.CountryQueryPort;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "state.create")
@Service
public class CreateStateService implements CreateStateUseCase {
    private final StateRepository repository;
    private final CountryQueryPort countryQueryPort;

    public CreateStateService(StateRepository repository, CountryQueryPort countryQueryPort) {
        this.repository = repository;
        this.countryQueryPort = countryQueryPort;
    }

    @Override
    public StateDto execute(CreateStateCommand command) {
        CountryRef country = countryQueryPort.findById(command.countryId()).orElseThrow(
                () -> new IllegalArgumentException("Country not found: " + command.countryId()));
        State state = State.create(command.name(), country, command.daneCode());
        return StateDto.from(repository.save(state));
    }
}
