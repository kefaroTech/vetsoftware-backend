package com.vetsoftware.app.animal.application.port.out;

import com.vetsoftware.app.animal.domain.AnimalColorRef;
import java.util.Optional;

public interface AnimalColorQueryPort {
    Optional<AnimalColorRef> findById(Long colorId);
}
