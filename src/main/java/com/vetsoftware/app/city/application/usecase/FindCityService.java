package com.vetsoftware.app.city.application.usecase;

import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.in.FindCityUseCase;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.domain.CityNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "city.find")
@Service
public class FindCityService implements FindCityUseCase {
    private final CityRepository repository;

    public FindCityService(CityRepository repository) {
        this.repository = repository;
    }

    @Override
    public CityDto findById(Long id) {
        return repository.findById(id).map(CityDto::from)
                .orElseThrow(() -> new CityNotFoundException(id));
    }
}
