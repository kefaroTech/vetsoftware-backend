package com.vetsoftware.app.country.application.usecase;

import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.in.FindCountryUseCase;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "country.find")
@Service
public class FindCountryService implements FindCountryUseCase {
    private final CountryRepository repository;

    public FindCountryService(CountryRepository repository) {
        this.repository = repository;
    }

    @Override
    public CountryDto findById(Long id) {
        return repository.findById(id).map(CountryDto::from)
                .orElseThrow(() -> new CountryNotFoundException(id));
    }
}
