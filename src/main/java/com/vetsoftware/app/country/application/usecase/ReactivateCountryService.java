package com.vetsoftware.app.country.application.usecase;

import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.in.ReactivateCountryUseCase;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "country.reactivate")
@Service
public class ReactivateCountryService implements ReactivateCountryUseCase {
    private final CountryRepository repository;

    public ReactivateCountryService(CountryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CountryDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new CountryNotFoundException(id);
        return CountryDto
                .from(repository.findById(id).orElseThrow(() -> new CountryNotFoundException(id)));
    }
}
