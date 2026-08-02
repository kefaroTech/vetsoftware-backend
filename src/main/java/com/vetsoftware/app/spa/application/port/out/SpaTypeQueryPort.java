package com.vetsoftware.app.spa.application.port.out;

import com.vetsoftware.app.spa.domain.SpaTypeRef;
import java.util.Optional;

public interface SpaTypeQueryPort {
  Optional<SpaTypeRef> findById(Long spaTypeId);
}
