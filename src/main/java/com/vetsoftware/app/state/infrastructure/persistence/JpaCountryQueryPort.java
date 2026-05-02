package com.vetsoftware.app.state.infrastructure.persistence;

import com.vetsoftware.app.country.infrastructure.persistence.CountryJpaRepository;
import com.vetsoftware.app.state.application.port.out.CountryQueryPort;
import com.vetsoftware.app.state.domain.CountryRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaCountryQueryPort implements CountryQueryPort {
    private final CountryJpaRepository countryJpaRepository;

    public JpaCountryQueryPort(CountryJpaRepository countryJpaRepository) {
        this.countryJpaRepository = countryJpaRepository;
    }

    @Override
    public Optional<CountryRef> findById(Long countryId) {
        return countryJpaRepository.findById(countryId)
            .map(e -> new CountryRef(e.getId(), e.getName()));
    }
}
