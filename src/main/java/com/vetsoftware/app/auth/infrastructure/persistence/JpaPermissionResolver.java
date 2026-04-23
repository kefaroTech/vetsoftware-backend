package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.PermissionResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPermissionResolver implements PermissionResolver {

    @PersistenceContext
    private EntityManager em;

    @Cacheable(value = "employee-permissions", key = "#employeeId")
    @Override
    public Set<String> resolveFor(Long employeeId) {
        List<String> codes = em.createNativeQuery("""
                SELECT p.code
                FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN roles r            ON r.id = rp.role_id
                JOIN employee_roles er  ON er.role_id = r.id
                WHERE er.employee_id = :employeeId
                """)
                .setParameter("employeeId", employeeId)
                .getResultList();
        return new HashSet<>(codes);
    }

    @CacheEvict(value = "employee-permissions", key = "#employeeId")
    public void evict(Long employeeId) {}
}
