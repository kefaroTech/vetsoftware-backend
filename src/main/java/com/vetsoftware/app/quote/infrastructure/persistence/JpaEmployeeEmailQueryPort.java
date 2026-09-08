package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.quote.application.port.out.EmployeeEmailQueryPort;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployeeEmailQueryPort implements EmployeeEmailQueryPort {

    private static final String SQL_EMAIL = """
            SELECT email FROM employees
             WHERE id = :employeeId AND company_id = :companyId AND enabled = TRUE
            """;

    private final EntityManager entityManager;

    public JpaEmployeeEmailQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<String> findEmail(Long employeeId, Long companyId) {
        List<?> rows = entityManager.createNativeQuery(SQL_EMAIL)
                .setParameter("employeeId", employeeId).setParameter("companyId", companyId)
                .setMaxResults(1).getResultList();
        return rows.isEmpty() || rows.get(0) == null
                ? Optional.empty()
                : Optional.of(String.valueOf(rows.get(0)));
    }
}
