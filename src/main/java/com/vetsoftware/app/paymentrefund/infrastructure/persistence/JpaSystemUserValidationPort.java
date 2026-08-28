package com.vetsoftware.app.paymentrefund.infrastructure.persistence;

import com.vetsoftware.app.paymentrefund.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code systemuser}.
 *
 * <p>
 * Se apoya solo en {@code existsById}, que es de {@code JpaRepository} y por
 * tanto no depende de ningun metodo derivado ni de ningun getter de la entidad
 * ajena.
 */
@Component("paymentRefundJpaSystemUserValidationPort")
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
