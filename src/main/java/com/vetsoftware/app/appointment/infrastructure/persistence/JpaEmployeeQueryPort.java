package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("appointmentJpaEmployeeQueryPort")
public class JpaEmployeeQueryPort implements EmployeeQueryPort {
    private final EmployeeJpaRepository employeeJpaRepository;
    private final EntityManager entityManager;

    public JpaEmployeeQueryPort(EmployeeJpaRepository employeeJpaRepository,
            EntityManager entityManager) {
        this.employeeJpaRepository = employeeJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId) {
        return employeeJpaRepository.findByIdAndCompany_Id(employeeId, companyId)
                .map(e -> new EmployeeRef(e.getId(), e.getName()));
    }

    /**
     * Vía {@link EntityManager} directa -no un método {@code @Lock} de
     * {@link EmployeeJpaRepository}- para que el {@code WHERE} acote por
     * {@code companyId} EN LA QUERY, antes de tomar el lock, y no comprobado
     * después en Java: es el mismo motivo por el que
     * {@code OpenAccountQueryPort.lockForUpdate} está acotado -ver el comentario de
     * {@code CreateDebtOpenAccountService.execute}-, porque la variante ancha
     * tomaría el {@code PESSIMISTIC_WRITE} sobre la fila de OTRO tenant antes de
     * cualquier comprobación.
     *
     * <p>
     * Granularidad: por empleado, no por (empleado, día). Bloquear solo el día
     * exigiría una fila propia por día -una tabla de agenda que hoy no existe en el
     * esquema-; bloquear la fila del empleado entero usa lo que ya existe y sigue
     * siendo correcto -serializa TODAS las escrituras de agenda de ese empleado
     * -alta, edición y reprogramación desde el issue #241-, cualquier día, mientras
     * dura la transacción-, aunque es más ancho de lo estrictamente necesario.
     * Documentado como decisión abierta en el issue de seguimiento.
     */
    @Override
    public void lockForOverlapCheck(Long employeeId, Long companyId) {
        try {
            entityManager.createQuery("""
                    SELECT e
                    FROM EmployeeJpaEntity e
                    WHERE e.id = :employeeId
                      AND e.company.id = :companyId
                    """, EmployeeJpaEntity.class).setParameter("employeeId", employeeId)
                    .setParameter("companyId", companyId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).getSingleResult();
        } catch (NoResultException ignored) {
            // Empleado inexistente o de otra empresa: el finder posterior
            // (findByIdAndCompanyId) es quien lanza el 400 de negocio, no este lock.
        }
    }
}
