package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.service.domain.TaxTreatment;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaEntity;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("serviceChargeOpenAccountJpaServiceQueryPort")
public class JpaServiceQueryPort implements ServiceQueryPort {
    private final ServiceJpaRepository serviceJpaRepository;

    public JpaServiceQueryPort(ServiceJpaRepository serviceJpaRepository) {
        this.serviceJpaRepository = serviceJpaRepository;
    }

    @Override
    public Optional<ServiceRef> findByIdAndCompanyId(Long serviceId, Long companyId) {
        return serviceJpaRepository.findByIdAndCompany_Id(serviceId, companyId).map(JpaServiceQueryPort::toRef);
    }

    private static ServiceRef toRef(ServiceJpaEntity e) {
        // Gravado tanto GRAVADO (IVA) como INC: ambos llevan impuesto. El esquema (IVA/INC) sale del Tax y
        // se congela en el cargo para que el documento del cierre lo respete igual que la venta POS.
        TaxTreatment treatment = e.getTaxTreatment();
        boolean hasTax = treatment == TaxTreatment.GRAVADO || treatment == TaxTreatment.INC;
        TaxJpaEntity t = e.getTax();
        TaxRef tax = hasTax && t != null
            ? new TaxRef(t.getId(), t.getName(), t.getPercentage(),
                t.getTaxScheme() == null ? null : t.getTaxScheme().name())
            : null;
        // Congela el tratamiento real del catálogo (incl. EXENTO/EXCLUIDO), no solo el hasTax monetario.
        return new ServiceRef(e.getId(), e.getName(), e.getPrice(), hasTax, tax,
            treatment == null ? null : treatment.name());
    }
}
