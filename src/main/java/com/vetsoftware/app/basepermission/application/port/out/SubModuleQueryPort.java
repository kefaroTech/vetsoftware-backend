package com.vetsoftware.app.basepermission.application.port.out;

import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import java.util.Optional;

public interface SubModuleQueryPort {
    Optional<SubModuleRef> findById(Long subModuleId);
}
