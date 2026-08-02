package com.vetsoftware.app.owner.application.port.out;

import com.vetsoftware.app.owner.domain.CityRef;
import java.util.Optional;

public interface CityQueryPort {
  Optional<CityRef> findById(Long cityId);
}
