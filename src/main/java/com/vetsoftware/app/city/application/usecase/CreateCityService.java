package com.vetsoftware.app.city.application.usecase;

import com.vetsoftware.app.city.application.command.CreateCityCommand;
import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.in.CreateCityUseCase;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.application.port.out.StateQueryPort;
import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.domain.StateRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "city.create")
@Service
public class CreateCityService implements CreateCityUseCase {
    private final CityRepository repository;
    private final StateQueryPort stateQueryPort;

    public CreateCityService(CityRepository repository, StateQueryPort stateQueryPort) {
        this.repository = repository;
        this.stateQueryPort = stateQueryPort;
    }

    @Override
    public CityDto execute(CreateCityCommand command) {
        StateRef state = stateQueryPort.findById(command.stateId())
            .orElseThrow(() -> new IllegalArgumentException("State not found: " + command.stateId()));
        City city = City.create(command.name(), state, command.daneCode());
        return CityDto.from(repository.save(city));
    }
}
