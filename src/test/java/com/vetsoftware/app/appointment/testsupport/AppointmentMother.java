package com.vetsoftware.app.appointment.testsupport;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.UpdateAppointmentCommand;
import com.vetsoftware.app.appointment.domain.AnimalRef;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.domain.BranchRef;
import com.vetsoftware.app.appointment.domain.CompanyRef;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import com.vetsoftware.app.appointment.domain.OwnerRef;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Fixtures del modulo appointment.
 *
 * <p>
 * Las citas se construyen con el constructor publico y nunca con
 * {@code Appointment.create(...)}: el factory estampa
 * {@code LocalDateTime.now()} en {@code createdDate} y volveria no
 * deterministas las aserciones sobre esa fecha.
 *
 * <p>
 * <b>Duracion (BE-17).</b> El caso por defecto deja {@code durationMinutes} a
 * {@code null} a proposito: es el valor que tienen todas las citas anteriores
 * al cambio y el que ejercita la herencia del ajuste de empresa. Para una cita
 * con duracion propia esta {@link #conDuracion(Integer)}.
 */
public final class AppointmentMother {

    public static final Long APPOINTMENT_ID = 55L;
    public static final Long COMPANY_ID = 9L;
    public static final Long EMPLOYEE_ID = 4L;
    public static final Long ANIMAL_ID = 100L;
    public static final Long OWNER_ID = 3L;
    public static final Long BRANCH_ID = 1L;

    /** Duracion propia de una cita, distinta del default para que se distingan. */
    public static final Integer DURACION = 45;

    /**
     * Sedes que el caller puede ver. Los ids de las citas cruzadas se filtran por
     * este conjunto antes de salir en el 409: el cruce se calcula por veterinario
     * —que puede tener agenda en varias sedes— pero solo se le puede contar al
     * caller lo de las suyas.
     */
    public static final Set<Long> SEDES_VISIBLES = Set.of(BRANCH_ID);

    /** Lo que devuelve la politica de empresa cuando no hay ajuste: 30 minutos. */
    public static final int DURACION_POR_DEFECTO = 30;

    public static final CompanyRef CLINICA = CompanyRef.of(COMPANY_ID);
    public static final BranchRef PRINCIPAL = new BranchRef(BRANCH_ID, "Principal", "PRINCIPAL");
    public static final BranchRef NORTE = new BranchRef(11L, "Sede Norte", "NORTE");
    public static final EmployeeRef VETERINARIA = new EmployeeRef(EMPLOYEE_ID, "Dra. Vet");
    public static final EmployeeRef OTRO_VETERINARIO = new EmployeeRef(5L, "Dr. Suplente");
    public static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    public static final OwnerRef DUENO = new OwnerRef(OWNER_ID, "Ana Ruiz");

    public static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 1, 9, 0);
    public static final LocalDateTime NUEVO_INICIO = LocalDateTime.of(2026, 8, 2, 15, 30);
    public static final LocalDateTime CREADA = LocalDateTime.of(2026, 7, 20, 8, 15);

    private AppointmentMother() {
    }

    /** Cita solicitada, con animal y dueno registrados. El caso por defecto. */
    public static Appointment solicitada() {
        return conEstado(AppointmentStatus.REQUESTED);
    }

    public static Appointment conEstado(AppointmentStatus status) {
        return new Appointment(APPOINTMENT_ID, INICIO, null, AppointmentType.CONSULTATION, status,
                "Control anual", null, FIRULAIS, DUENO, null, null, null, VETERINARIA, CLINICA,
                PRINCIPAL, 3L, true, CREADA);
    }

    /** Cita con duracion propia: no hereda la de la empresa. */
    public static Appointment conDuracion(Integer durationMinutes) {
        return new Appointment(APPOINTMENT_ID, INICIO, durationMinutes,
                AppointmentType.CONSULTATION, AppointmentStatus.REQUESTED, "Control anual", null,
                FIRULAIS, DUENO, null, null, null, VETERINARIA, CLINICA, PRINCIPAL, 3L, true,
                CREADA);
    }

    /** Cita de contacto libre: sin animal ni dueno registrados. */
    public static Appointment deContactoLibre(String clientEmail) {
        return new Appointment(APPOINTMENT_ID, INICIO, null, AppointmentType.GROOMING,
                AppointmentStatus.REQUESTED, null, null, null, null, "Walk-in", "3001234567",
                clientEmail, VETERINARIA, CLINICA, PRINCIPAL, 0L, true, CREADA);
    }

    public static Appointment cancelada(String motivo) {
        return new Appointment(APPOINTMENT_ID, INICIO, null, AppointmentType.CONSULTATION,
                AppointmentStatus.CANCELLED, "Control anual", motivo, FIRULAIS, DUENO, null, null,
                null, VETERINARIA, CLINICA, PRINCIPAL, 4L, false, CREADA);
    }

    /** Alta sin duracion propia y sin forzar el solape. El caso por defecto. */
    public static CreateAppointmentCommand comandoDeCreacion() {
        return comandoDeCreacion(null, false);
    }

    public static CreateAppointmentCommand comandoDeCreacion(Integer durationMinutes,
            boolean forceOverlap) {
        return new CreateAppointmentCommand(INICIO, durationMinutes, AppointmentType.CONSULTATION,
                EMPLOYEE_ID, ANIMAL_ID, OWNER_ID, null, null, null, "Control anual", BRANCH_ID,
                COMPANY_ID, forceOverlap, SEDES_VISIBLES);
    }

    /** Edicion sin duracion propia y sin forzar el solape. El caso por defecto. */
    public static UpdateAppointmentCommand comandoDeActualizacion() {
        return comandoDeActualizacion(null, false);
    }

    public static UpdateAppointmentCommand comandoDeActualizacion(Integer durationMinutes,
            boolean forceOverlap) {
        return new UpdateAppointmentCommand(APPOINTMENT_ID, NUEVO_INICIO, durationMinutes,
                AppointmentType.SURGERY, EMPLOYEE_ID, ANIMAL_ID, OWNER_ID, null, null, null,
                "Reprogramada", COMPANY_ID, forceOverlap, SEDES_VISIBLES);
    }

    /**
     * Reprogramacion al veterinario suplente, sin tocar la duracion (PATCH: el
     * {@code null} conserva la que la cita ya tenia) y sin forzar el solape.
     */
    public static RescheduleAppointmentCommand comandoDeReprogramacion() {
        return comandoDeReprogramacion(null, false);
    }

    public static RescheduleAppointmentCommand comandoDeReprogramacion(Integer durationMinutes,
            boolean forceOverlap) {
        return new RescheduleAppointmentCommand(APPOINTMENT_ID, NUEVO_INICIO, durationMinutes,
                OTRO_VETERINARIO.id(), COMPANY_ID, forceOverlap, SEDES_VISIBLES);
    }
}
