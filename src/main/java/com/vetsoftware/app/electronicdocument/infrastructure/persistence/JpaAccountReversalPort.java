package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.AccountReversalPort;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Adapter del {@link AccountReversalPort}: marca la cuenta abierta facturada como reversada cuando su
 * nota credito es validada por la DIAN. Cruce permitido (persistence -> persistence de otra feature).
 * Idempotente: no re-estampa una cuenta ya reversada ni falla si la cuenta no existe.
 */
@Component
public class JpaAccountReversalPort implements AccountReversalPort {
    private final OpenAccountJpaRepository openAccountJpaRepository;

    public JpaAccountReversalPort(OpenAccountJpaRepository openAccountJpaRepository) {
        this.openAccountJpaRepository = openAccountJpaRepository;
    }

    @Override
    public void markReversed(Long openAccountId) {
        OpenAccountJpaEntity account = openAccountJpaRepository.findById(openAccountId).orElse(null);
        if (account == null || account.isReversed()) return;
        account.setReversed(true);
        account.setReversedAt(LocalDateTime.now());
        openAccountJpaRepository.save(account);
    }
}
