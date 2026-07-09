package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.OwnerRef;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("appointmentJpaOwnerQueryPort")
public class JpaOwnerQueryPort implements OwnerQueryPort {
    private final OwnerJpaRepository ownerJpaRepository;

    public JpaOwnerQueryPort(OwnerJpaRepository ownerJpaRepository) {
        this.ownerJpaRepository = ownerJpaRepository;
    }

    @Override
    public Optional<OwnerRef> findByIdAndCompanyId(Long ownerId, Long companyId) {
        return ownerJpaRepository.findByIdAndCompanyId(ownerId, companyId)
            .map(e -> new OwnerRef(e.getId(), e.getName()));
    }
}
