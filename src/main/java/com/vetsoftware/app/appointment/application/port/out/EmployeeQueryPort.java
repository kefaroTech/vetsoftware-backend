package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId);

    /**
     * Issue #114: bloqueo pesimista ACOTADO por empresa sobre la fila del empleado,
     * como primera sentencia de los TRES casos de uso que escriben agenda
     * -{@code CreateAppointmentService}, {@code UpdateAppointmentService} y
     * {@code RescheduleAppointmentService}, issue #241-, antes de cualquier
     * lectura, incluida esta misma. Serializa el read-then-write de dos escrituras
     * concurrentes sobre el MISMO empleado: la segunda transacción espera hasta que
     * la primera confirma (o revierte), y entonces ve la cita que la primera acaba
     * de dejar. Cubre los solapes PARCIALES que
     * {@code uq_appointments_active_employee_start} no ve (start_at distinto) y,
     * desde el issue #240, también los EXACTOS contra una cita forzada: esa
     * renuncia a su hueco en el índice y la base deja de arbitrarla.
     *
     * <p>
     * <b>Qué empleado se bloquea cuando la escritura cambia de profesional.</b>
     * Solo el DESTINO, el que viene en el command. Una agenda solo gana solapes por
     * las escrituras que meten una cita EN ella, y todas pasan por este lock; la de
     * origen no puede cruzarse por perder una cita. Bloquear las dos exigiría leer
     * la cita antes de tomar el primer lock -el patrón que este método existe para
     * cerrar- y pondría dos locks por transacción, con el interbloqueo cruzado que
     * documenta el issue #229. Con uno solo no hay ciclo de espera posible.
     * Contrapartida aceptada: mientras una transacción mueve una cita FUERA de la
     * agenda de A sin bloquear a A, otra que escriba en A puede ver todavía esa
     * cita y responder un 409 de solape que un reintento ya no da. Es un falso
     * rechazo transitorio, nunca un doble booking.
     *
     * <p>
     * No lanza si el empleado no existe o no es de esta empresa -eso lo decide
     * {@link #findByIdAndCompanyId}, que corre después-; este método solo bloquea.
     */
    void lockForOverlapCheck(Long employeeId, Long companyId);
}
