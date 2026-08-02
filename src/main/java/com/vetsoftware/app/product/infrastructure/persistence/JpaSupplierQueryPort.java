package com.vetsoftware.app.product.infrastructure.persistence;

import com.vetsoftware.app.product.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.product.domain.SupplierRef;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("productJpaSupplierQueryPort")
public class JpaSupplierQueryPort implements SupplierQueryPort {
    private final SupplierJpaRepository supplierJpaRepository;

    public JpaSupplierQueryPort(SupplierJpaRepository supplierJpaRepository) {
        this.supplierJpaRepository = supplierJpaRepository;
    }

    @Override
    public Optional<SupplierRef> findById(Long supplierId, Long companyId) {
        return supplierJpaRepository.findByIdAndCompany_Id(supplierId, companyId)
                .map(e -> new SupplierRef(e.getId(), e.getName()));
    }
}
