package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code systemuser}.
 *
 * <p>
 * Se apoya solo en {@code existsById}, que es de {@code JpaRepository} y por
 * tanto no depende de ningun metodo derivado ni de ningun getter de la entidad
 * ajena.
 *
 * <p>
 * El nombre del bean va cualificado: {@code paymentrefund} y otros slices
 * declaran su propio {@code JpaSystemUserValidationPort} con el mismo nombre
 * simple.
 */
@Component("externalInvoiceReconciliationJpaSystemUserValidationPort")
public class JpaSystemUserValidationPort implements SystemUserValidationPort {

    private final SystemUserJpaRepository systemUserJpaRepository;

    public JpaSystemUserValidationPort(SystemUserJpaRepository systemUserJpaRepository) {
        this.systemUserJpaRepository = systemUserJpaRepository;
    }

    @Override
    public boolean existsById(Long systemUserId) {
        return systemUserId != null && systemUserJpaRepository.existsById(systemUserId);
    }
}
