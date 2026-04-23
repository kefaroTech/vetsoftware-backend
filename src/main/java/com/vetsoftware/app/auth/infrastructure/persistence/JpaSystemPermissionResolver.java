package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.SystemPermissionResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemPermissionResolver implements SystemPermissionResolver {

    @PersistenceContext
    private EntityManager em;

    @Cacheable(value = "system-user-permissions", key = "#systemUserId")
    @Override
    public Set<String> resolveFor(Long systemUserId) {
        List<String> codes = em.createNativeQuery("""
                SELECT sp.code
                FROM system_permissions sp
                JOIN system_user_permissions sup ON sup.system_permission_id = sp.id
                WHERE sup.system_user_id = :systemUserId
                """)
                .setParameter("systemUserId", systemUserId)
                .getResultList();
        return new HashSet<>(codes);
    }

    @CacheEvict(value = "system-user-permissions", key = "#systemUserId")
    public void evict(Long systemUserId) {}
}
