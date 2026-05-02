package com.vetsoftware.app.state.application.port.out;

import com.vetsoftware.app.state.domain.CountryRef;
import java.util.Optional;

public interface CountryQueryPort {
    Optional<CountryRef> findById(Long countryId);
}
