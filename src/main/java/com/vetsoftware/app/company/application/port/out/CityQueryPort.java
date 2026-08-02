package com.vetsoftware.app.company.application.port.out;

import com.vetsoftware.app.company.domain.CityRef;
import java.util.Optional;

public interface CityQueryPort {
  Optional<CityRef> findById(Long cityId);
}
