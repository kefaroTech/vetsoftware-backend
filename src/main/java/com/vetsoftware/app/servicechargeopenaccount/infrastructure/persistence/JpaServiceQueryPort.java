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
    public Optional<ServiceRef> findById(Long serviceId) {
        return serviceJpaRepository.findById(serviceId).map(JpaServiceQueryPort::toRef);
    }

    private static ServiceRef toRef(ServiceJpaEntity e) {
        boolean hasTax = e.getTaxTreatment() == TaxTreatment.GRAVADO;
        TaxJpaEntity t = e.getTax();
        TaxRef tax = hasTax && t != null ? new TaxRef(t.getId(), t.getName(), t.getPercentage()) : null;
        return new ServiceRef(e.getId(), e.getName(), e.getPrice(), hasTax, tax);
    }
}
