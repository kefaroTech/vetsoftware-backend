package com.vetsoftware.app.supplierinvoice.infrastructure.persistence;

import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaRepository;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("supplierInvoiceJpaSupplierQueryPort")
public class JpaSupplierQueryPort implements SupplierQueryPort {
    private final SupplierJpaRepository supplierJpaRepository;

    public JpaSupplierQueryPort(SupplierJpaRepository supplierJpaRepository) {
        this.supplierJpaRepository = supplierJpaRepository;
    }

    @Override
    public Optional<SupplierRef> findByIdAndCompanyId(Long supplierId, Long companyId) {
        return supplierJpaRepository.findByIdAndCompany_Id(supplierId, companyId)
            .map(e -> new SupplierRef(e.getId(), e.getName(), e.getTaxId()));
    }
}
