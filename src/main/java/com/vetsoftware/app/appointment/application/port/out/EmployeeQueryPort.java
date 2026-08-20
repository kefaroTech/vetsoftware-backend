package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId);

    /**
     * Issue #114: bloqueo pesimista ACOTADO por empresa sobre la fila del empleado,
     * como primera sentencia de {@code CreateAppointmentService.execute} -antes de
     * cualquier lectura, incluida esta misma-. Serializa el read-then-write de dos
     * creaciones de cita concurrentes sobre el MISMO empleado: la segunda
     * transacción espera hasta que la primera confirma (o revierte), y entonces ve
     * el solape que la primera acaba de crear. Cubre los solapes PARCIALES que
     * {@code uq_appointments_active_employee_start} no ve (start_at distinto).
     *
     * <p>
     * No lanza si el empleado no existe o no es de esta empresa -eso lo decide
     * {@link #findByIdAndCompanyId}, que corre después-; este método solo bloquea.
     */
    void lockForOverlapCheck(Long employeeId, Long companyId);
}
