package com.vetsoftware.app.country.application.usecase;

import com.vetsoftware.app.country.application.command.CreateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.in.CreateCountryUseCase;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.Country;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "country.create")
@Service
public class CreateCountryService implements CreateCountryUseCase {
  private final CountryRepository repository;

  public CreateCountryService(CountryRepository repository) {
    this.repository = repository;
  }

  @Override
  public CountryDto execute(CreateCountryCommand command) {
    return CountryDto.from(repository.save(Country.create(command.name())));
  }
}
