package com.vetsoftware.app.rolepermission.application.port.out;

import com.vetsoftware.app.rolepermission.domain.RoleRef;
import java.util.Optional;

public interface RoleQueryPort {
    Optional<RoleRef> findById(Long roleId);
    Optional<RoleRef> findByIdAndCompanyId(Long roleId, Long companyId);
}
