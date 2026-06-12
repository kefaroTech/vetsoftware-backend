package com.vetsoftware.app.companytaxprofile.application.port.out;

import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import java.util.Optional;

public interface EconomicActivityQueryPort {
    Optional<EconomicActivityRef> findById(Long economicActivityId);
}
