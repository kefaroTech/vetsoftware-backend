package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeCredentialsRepository implements EmployeeCredentialsRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<EmployeeCredentials> findByCode(String employeeCode) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT id, hash_password
                FROM employees
                WHERE employee_code = :code
                AND status = 'ACTIVE'
                """)
                .setParameter("code", employeeCode)
                .getResultList();

        return rows.stream()
                .findFirst()
                .map(row -> new EmployeeCredentials(
                        ((Number) row[0]).longValue(),
                        (String) row[1]
                ));
    }
}
