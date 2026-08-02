package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserQueryPort;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("systemuserpermissionJpaSystemUserQueryPort")
public class JpaSystemUserQueryPort implements SystemUserQueryPort {
    private final SystemUserJpaRepository systemUserJpaRepository;

    public JpaSystemUserQueryPort(SystemUserJpaRepository systemUserJpaRepository) {
        this.systemUserJpaRepository = systemUserJpaRepository;
    }

    @Override
    public Optional<SystemUserRef> findById(Long systemUserId) {
        return systemUserJpaRepository.findById(systemUserId)
                .map(e -> new SystemUserRef(e.getId(), e.getCode()));
    }
}
