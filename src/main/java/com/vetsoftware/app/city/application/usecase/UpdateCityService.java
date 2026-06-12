package com.vetsoftware.app.city.application.usecase;

import com.vetsoftware.app.city.application.command.UpdateCityCommand;
import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.in.UpdateCityUseCase;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.application.port.out.StateQueryPort;
import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.domain.CityNotFoundException;
import com.vetsoftware.app.city.domain.StateRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "city.update")
@Service
public class UpdateCityService implements UpdateCityUseCase {
    private final CityRepository repository;
    private final StateQueryPort stateQueryPort;

    public UpdateCityService(CityRepository repository, StateQueryPort stateQueryPort) {
        this.repository = repository;
        this.stateQueryPort = stateQueryPort;
    }

    @Override
    @Transactional
    public CityDto execute(UpdateCityCommand command) {
        City city = repository.findById(command.id())
            .orElseThrow(() -> new CityNotFoundException(command.id()));
        StateRef state = stateQueryPort.findById(command.stateId())
            .orElseThrow(() -> new IllegalArgumentException("State not found: " + command.stateId()));
        city.update(command.name(), state, command.daneCode());
        return CityDto.from(repository.save(city));
    }
}
