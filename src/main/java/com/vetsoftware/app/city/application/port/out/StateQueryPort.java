package com.vetsoftware.app.city.application.port.out;

import com.vetsoftware.app.city.domain.StateRef;
import java.util.Optional;

public interface StateQueryPort {
    Optional<StateRef> findById(Long stateId);
}
