package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.SystemPermissionResolver;
import java.util.HashSet;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemPermissionResolver implements SystemPermissionResolver {

    private final SystemUserPermissionJpaRepository systemUserPermissionJpaRepository;

    public JpaSystemPermissionResolver(
            SystemUserPermissionJpaRepository systemUserPermissionJpaRepository) {
        this.systemUserPermissionJpaRepository = systemUserPermissionJpaRepository;
    }

    @Cacheable(value = "system-user-permissions", key = "#systemUserId")
    @Override
    public Set<String> resolveFor(Long systemUserId) {
        return new HashSet<>(
                systemUserPermissionJpaRepository.findPermissionCodesBySystemUserId(systemUserId));
    }

    @CacheEvict(value = "system-user-permissions", key = "#systemUserId")
    public void evict(Long systemUserId) {}
}
