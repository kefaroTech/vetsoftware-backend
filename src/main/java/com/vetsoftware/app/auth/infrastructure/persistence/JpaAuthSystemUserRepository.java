package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuthSystemUserRepository implements AuthSystemUserRepository {

    private final SystemUserJpaRepository systemUserJpaRepository;

    public JpaAuthSystemUserRepository(SystemUserJpaRepository systemUserJpaRepository) {
        this.systemUserJpaRepository = systemUserJpaRepository;
    }

    @Override
    public Optional<AuthSystemUser> findActiveById(Long systemUserId) {
        return systemUserJpaRepository.findById(systemUserId)
                .map(user -> new AuthSystemUser(user.getId(), user.getAuthVersion()));
    }

    @Override
    public Optional<AuthSystemUser> rotateAuthVersion(Long systemUserId) {
        return systemUserJpaRepository.findByIdForUpdate(systemUserId).map(user -> {
            long nextVersion = user.getAuthVersion() + 1L;
            user.setAuthVersion(nextVersion);
            systemUserJpaRepository.saveAndFlush(user);
            return new AuthSystemUser(user.getId(), nextVersion);
        });
    }

    @Override
    public void bumpAuthVersion(Long systemUserId) {
        systemUserJpaRepository.bumpAuthVersion(systemUserId);
    }
}
