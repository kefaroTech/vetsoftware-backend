package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.PermissionResolver;
import java.util.HashSet;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPermissionResolver implements PermissionResolver {

    private final EmployeeRolePermissionJpaRepository employeeRolePermissionJpaRepository;

    public JpaPermissionResolver(
            EmployeeRolePermissionJpaRepository employeeRolePermissionJpaRepository) {
        this.employeeRolePermissionJpaRepository = employeeRolePermissionJpaRepository;
    }

    @Cacheable(value = "employee-permissions", key = "#employeeId")
    @Override
    public Set<String> resolveFor(Long employeeId) {
        return new HashSet<>(
                employeeRolePermissionJpaRepository.findPermissionCodesByEmployeeId(employeeId));
    }

    @CacheEvict(value = "employee-permissions", key = "#employeeId")
    public void evict(Long employeeId) {}
}
