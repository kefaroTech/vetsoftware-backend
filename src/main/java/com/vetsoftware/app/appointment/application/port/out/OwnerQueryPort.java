package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.domain.OwnerRef;
import java.util.Optional;

public interface OwnerQueryPort {
    Optional<OwnerRef> findByIdAndCompanyId(Long ownerId, Long companyId);

    /**
     * Correo del propietario (para notificarle la cita). Vacío si no tiene correo o
     * no pertenece a la empresa.
     */
    Optional<String> findEmailByIdAndCompanyId(Long ownerId, Long companyId);
}
