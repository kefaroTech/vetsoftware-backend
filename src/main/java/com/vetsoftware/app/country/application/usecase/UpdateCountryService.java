package com.vetsoftware.app.country.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.country.application.command.UpdateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.in.UpdateCountryUseCase;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.Country;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "country.update")
@Service
public class UpdateCountryService implements UpdateCountryUseCase {
    private final CountryRepository repository;

    public UpdateCountryService(CountryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CountryDto execute(UpdateCountryCommand command, AuthContext auth) {
        Country country = repository.findById(command.id())
                .orElseThrow(() -> new CountryNotFoundException(command.id()));
        country.update(command.name());
        return CountryDto.from(repository.save(country));
    }
}
