package com.vetsoftware.app.surgery.application.port.out;

import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import java.util.Optional;

public interface SurgeryTypeQueryPort {
    Optional<SurgeryTypeRef> findById(Long surgeryTypeId);
}
