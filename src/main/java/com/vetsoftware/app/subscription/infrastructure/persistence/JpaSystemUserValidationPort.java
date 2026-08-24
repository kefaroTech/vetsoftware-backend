package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import org.springframework.stereotype.Component;

@Component("subscriptionJpaSystemUserValidationPort")
public class JpaSystemUserValidationPort implements SystemUserValidationPort {

    private final SystemUserJpaRepository systemUserJpaRepository;

    public JpaSystemUserValidationPort(SystemUserJpaRepository systemUserJpaRepository) {
        this.systemUserJpaRepository = systemUserJpaRepository;
    }

    @Override
    public void validateExists(Long systemUserId) {
        if (systemUserId == null || !systemUserJpaRepository.existsById(systemUserId)) {
            throw new IllegalArgumentException("System user not found: " + systemUserId);
        }
    }
}
