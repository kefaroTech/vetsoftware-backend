package com.vetsoftware.app.servicechargeopenaccount.application.port.out;

import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
    Optional<AnimalRef> findById(Long animalId);
}
