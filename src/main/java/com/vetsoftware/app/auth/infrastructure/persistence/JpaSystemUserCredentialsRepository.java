package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemUserCredentialsRepository implements SystemUserCredentialsRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<SystemUserCredentials> findByCode(String code) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT id, hash_password
                FROM system_users
                WHERE code = :code
                """)
                .setParameter("code", code)
                .getResultList();

        return rows.stream()
                .findFirst()
                .map(row -> new SystemUserCredentials(
                        ((Number) row[0]).longValue(),
                        (String) row[1]
                ));
    }
}
