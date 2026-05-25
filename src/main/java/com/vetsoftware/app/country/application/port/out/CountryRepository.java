package com.vetsoftware.app.country.application.port.out;

import com.vetsoftware.app.country.domain.Country;
import java.util.List;
import java.util.Optional;

public interface CountryRepository {
    Country save(Country country);
    Optional<Country> findById(Long id);
    List<Country> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
