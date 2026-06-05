package com.vetsoftware.app.servicechargeopenaccount.application.port.out;

import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import java.util.Optional;

public interface ServiceQueryPort {
    Optional<ServiceRef> findById(Long serviceId);
}
