package com.vetsoftware.app.animal.application.port.out;

import com.vetsoftware.app.animal.domain.OwnerRef;
import java.util.Optional;

public interface OwnerQueryPort {
    Optional<OwnerRef> findByIdAndCompanyId(Long ownerId, Long companyId);
}
