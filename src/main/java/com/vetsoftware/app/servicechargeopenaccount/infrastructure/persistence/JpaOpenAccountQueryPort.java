package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("serviceChargeOpenAccountJpaOpenAccountQueryPort")
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
    public Optional<OpenAccountRef> findByIdAndCompanyId(Long openAccountId, Long companyId) {
        return openAccountJpaRepository.findByIdAndCompany_Id(openAccountId, companyId)
                .map(e -> new OpenAccountRef(e.getId(), e.getCompany().getId()));
    }

    @Override
    public void lockForUpdate(Long openAccountId, Long companyId) {
        // Variante scoped: el FOR UPDATE solo toma el lock si la fila pertenece a la
        // empresa. Con findByIdForUpdate (ancha) se bloqueaba la cuenta de otro tenant
        // durante lo que durara la transaccion, antes de cualquier comprobacion.
        openAccountJpaRepository.findByIdForUpdateAndCompanyId(openAccountId, companyId);
    }

    @Override
    public boolean isOpen(Long openAccountId) {
        return openAccountJpaRepository.findById(openAccountId)
                .map(e -> e.getStatus() == OpenAccountStatus.OPEN).orElse(false);
    }

    @Override
    public BigDecimal outstandingAmount(Long openAccountId) {
        return openAccountJpaRepository.findById(openAccountId).map(
                com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity::getOutstandingAmount)
                .orElse(BigDecimal.ZERO);
    }
}
