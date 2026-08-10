package com.vetsoftware.app.appointment.testsupport;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
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

/**
 * Fixtures del modulo appointment.
 *
 * <p>
 * Las citas se construyen con el constructor publico y nunca con
 * {@code Appointment.create(...)}: el factory estampa
 * {@code LocalDateTime.now()} en {@code createdDate} y volveria no
 * deterministas las aserciones sobre esa fecha.
 */
public final class AppointmentMother {

    public static final Long APPOINTMENT_ID = 55L;
    public static final Long COMPANY_ID = 9L;
    public static final Long EMPLOYEE_ID = 4L;
    public static final Long ANIMAL_ID = 100L;
    public static final Long OWNER_ID = 3L;
    public static final Long BRANCH_ID = 1L;

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
        return new Appointment(APPOINTMENT_ID, INICIO, AppointmentType.CONSULTATION, status,
                "Control anual", null, FIRULAIS, DUENO, null, null, null, VETERINARIA, CLINICA,
                PRINCIPAL, 3L, true, CREADA);
    }

    /** Cita de contacto libre: sin animal ni dueno registrados. */
    public static Appointment deContactoLibre(String clientEmail) {
        return new Appointment(APPOINTMENT_ID, INICIO, AppointmentType.GROOMING,
                AppointmentStatus.REQUESTED, null, null, null, null, "Walk-in", "3001234567",
                clientEmail, VETERINARIA, CLINICA, PRINCIPAL, 0L, true, CREADA);
    }

    public static Appointment cancelada(String motivo) {
        return new Appointment(APPOINTMENT_ID, INICIO, AppointmentType.CONSULTATION,
                AppointmentStatus.CANCELLED, "Control anual", motivo, FIRULAIS, DUENO, null, null,
                null, VETERINARIA, CLINICA, PRINCIPAL, 4L, false, CREADA);
    }

    public static CreateAppointmentCommand comandoDeCreacion() {
        return new CreateAppointmentCommand(INICIO, AppointmentType.CONSULTATION, EMPLOYEE_ID,
                ANIMAL_ID, OWNER_ID, null, null, null, "Control anual", BRANCH_ID, COMPANY_ID);
    }

    public static UpdateAppointmentCommand comandoDeActualizacion() {
        return new UpdateAppointmentCommand(APPOINTMENT_ID, NUEVO_INICIO, AppointmentType.SURGERY,
                EMPLOYEE_ID, ANIMAL_ID, OWNER_ID, null, null, null, "Reprogramada", COMPANY_ID);
    }
}
