package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.domain.OwnerRef;
import java.util.Optional;

public interface OwnerQueryPort {
    Optional<OwnerRef> findByIdAndCompanyId(Long ownerId, Long companyId);
}
