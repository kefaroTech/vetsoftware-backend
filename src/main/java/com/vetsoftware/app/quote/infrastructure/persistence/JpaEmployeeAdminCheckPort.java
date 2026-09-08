package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.quote.application.port.out.EmployeeAdminCheckPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/**
 * Resuelve el rol base {@code ADMIN} por SQL nativo contra
 * {@code employee_roles}/{@code roles}, sin importar clases de la feature
 * {@code employeerole}/{@code role} — mismo patrón que
 * {@code JpaCatalogQueryPorts}: el contrato es el esquema.
 */
@Component
public class JpaEmployeeAdminCheckPort implements EmployeeAdminCheckPort {

    private static final String SQL_IS_ADMIN = """
            SELECT COUNT(*)
              FROM employee_roles er
              JOIN roles r ON r.id = er.role_id
             WHERE er.employee_id = :employeeId
               AND r.company_id = :companyId
               AND r.code = 'ADMIN'
               AND er.enabled = TRUE
               AND r.enabled = TRUE
            """;

    private final EntityManager entityManager;

    public JpaEmployeeAdminCheckPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean isCompanyAdmin(Long employeeId, Long companyId) {
        if (employeeId == null || companyId == null)
            return false;
        Object result = entityManager.createNativeQuery(SQL_IS_ADMIN)
                .setParameter("employeeId", employeeId).setParameter("companyId", companyId)
                .getSingleResult();
        return ((Number) result).intValue() > 0;
    }
}
