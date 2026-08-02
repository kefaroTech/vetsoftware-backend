package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("debtOpenAccountJpaOpenAccountQueryPort")
public class JpaOpenAccountQueryPort implements OpenAccountQueryPort {
    private final OpenAccountJpaRepository openAccountJpaRepository;

    public JpaOpenAccountQueryPort(OpenAccountJpaRepository openAccountJpaRepository) {
        this.openAccountJpaRepository = openAccountJpaRepository;
    }

    @Override
    public Optional<OpenAccountRef> findById(Long openAccountId) {
        return openAccountJpaRepository.findById(openAccountId)
                .map(e -> new OpenAccountRef(e.getId(), e.getCompany().getId()));
    }

    @Override
    public void lockForUpdate(Long openAccountId) {
        openAccountJpaRepository.findByIdForUpdate(openAccountId);
    }

    @Override
    public boolean isOpen(Long openAccountId) {
        return openAccountJpaRepository.findById(openAccountId)
                .map(e -> e.getStatus() == OpenAccountStatus.OPEN).orElse(false);
    }

    @Override
    public BigDecimal outstandingAmount(Long openAccountId) {
        return openAccountJpaRepository.findById(openAccountId)
                .map(OpenAccountJpaEntity::getOutstandingAmount).orElse(BigDecimal.ZERO);
    }
}
