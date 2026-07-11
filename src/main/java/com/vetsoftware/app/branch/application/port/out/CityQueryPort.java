package com.vetsoftware.app.branch.application.port.out;

import com.vetsoftware.app.branch.domain.CityRef;
import java.util.Optional;

public interface CityQueryPort {
    Optional<CityRef> findById(Long cityId);
}
