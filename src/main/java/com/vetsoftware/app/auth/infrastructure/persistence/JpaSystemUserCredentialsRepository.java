package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemUserCredentialsRepository implements SystemUserCredentialsRepository {

    private final SystemUserJpaRepository systemUserJpaRepository;

    public JpaSystemUserCredentialsRepository(SystemUserJpaRepository systemUserJpaRepository) {
        this.systemUserJpaRepository = systemUserJpaRepository;
    }

    @Override
    public Optional<SystemUserCredentials> findByCode(String code) {
        return systemUserJpaRepository.findByCode(code)
                .map(u -> new SystemUserCredentials(u.getId(), u.getHashPassword()));
    }
}
