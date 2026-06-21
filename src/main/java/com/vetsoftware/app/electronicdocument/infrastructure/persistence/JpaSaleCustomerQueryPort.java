package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.SaleCustomerQueryPort;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.TaxRegime;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter: carga el Owner (scoped a la empresa) y lo congela en un CustomerSnapshot, igual que el flujo de
 * cuenta cerrada. Devuelve vacio si el owner no existe o no es de la empresa.
 */
@Component
public class JpaSaleCustomerQueryPort implements SaleCustomerQueryPort {
    private final OwnerJpaRepository ownerRepository;

    public JpaSaleCustomerQueryPort(OwnerJpaRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    public Optional<SaleCustomer> findOwner(Long ownerId, Long companyId) {
        OwnerJpaEntity owner = ownerRepository.findById(ownerId).orElse(null);
        if (owner == null || owner.getCompany() == null || !companyId.equals(owner.getCompany().getId())) {
            return Optional.empty();
        }
        CustomerSnapshot snapshot = new CustomerSnapshot(
                owner.getDocumentType() == null ? null : owner.getDocumentType().name(),
                owner.getDocument(), owner.getVerificationDigit(),
                owner.getPersonType() == null ? null : owner.getPersonType().name(),
                owner.getLegalName(), owner.getName(), owner.getEmail(),
                owner.getCity() == null ? null : owner.getCity().getDaneCode(),
                TaxRegime.fromName(owner.getTaxRegime() == null ? null : owner.getTaxRegime().name()));
        return Optional.of(new SaleCustomer(snapshot, owner.isWithholdingAgent()));
    }
}
