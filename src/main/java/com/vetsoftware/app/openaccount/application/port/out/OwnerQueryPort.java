package com.vetsoftware.app.openaccount.application.port.out;

import com.vetsoftware.app.openaccount.domain.OwnerRef;
import java.util.Optional;

public interface OwnerQueryPort {
    /**
     * Resuelve el propietario SOLO dentro de la empresa. No hay variante ancha a
     * proposito: el {@code ownerId} lo elige el cliente en el request, y sin acotar
     * se abria una cuenta de mi empresa colgada del propietario de la vecina. Al
     * quedarse los dos llamadores con esta, la ancha se borro para que la fuga sea
     * inexpresable, no solo esta vez.
     */
    Optional<OwnerRef> findByIdAndCompanyId(Long ownerId, Long companyId);
}
