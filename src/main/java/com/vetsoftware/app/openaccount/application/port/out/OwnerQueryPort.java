package com.vetsoftware.app.openaccount.application.port.out;

import com.vetsoftware.app.openaccount.domain.OwnerRef;
import java.util.Optional;

public interface OwnerQueryPort {
    Optional<OwnerRef> findById(Long ownerId);
}
