package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.country.application.dto.CountryDto;
import java.util.List;

public interface ListCountriesUseCase {
  List<CountryDto> listAll();
}
