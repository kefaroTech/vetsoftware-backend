package com.vetsoftware.app.city.application.usecase;

import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.in.ListCitiesByStateUseCase;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "city.listByState")
@Service
public class ListCitiesByStateService implements ListCitiesByStateUseCase {
    private final CityRepository repository;

    public ListCitiesByStateService(CityRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CityDto> listByState(Long stateId) {
        return repository.findByStateId(stateId).stream().map(CityDto::from).toList();
    }
}
