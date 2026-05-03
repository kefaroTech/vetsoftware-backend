package com.vetsoftware.app.country.application.usecase;

import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.in.ListCountriesUseCase;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "country.list")
@Service
public class ListCountriesService implements ListCountriesUseCase {
    private final CountryRepository repository;

    public ListCountriesService(CountryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CountryDto> listAll() {
        return repository.findAll().stream().map(CountryDto::from).toList();
    }
}
