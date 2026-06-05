package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
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
        return serviceJpaRepository.findById(serviceId)
            .map(e -> new ServiceRef(e.getId(), e.getName(), e.getPrice()));
    }
}
