package com.vetsoftware.app.permission.application.port.out;

import com.vetsoftware.app.permission.domain.SubModuleRef;
import java.util.Optional;

public interface SubModuleQueryPort {
    Optional<SubModuleRef> findById(Long subModuleId);
}
