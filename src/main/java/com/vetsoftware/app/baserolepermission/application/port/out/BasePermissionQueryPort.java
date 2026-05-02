package com.vetsoftware.app.baserolepermission.application.port.out;

import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import java.util.Optional;

public interface BasePermissionQueryPort {
    Optional<BasePermissionRef> findById(Long basePermissionId);
}
