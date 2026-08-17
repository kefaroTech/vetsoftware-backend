package com.vetsoftware.app.appointment.infrastructure.web.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateAppointmentRequest(@NotNull LocalDateTime startAt,
        // Opcional: duración en minutos. Si no se envía, se usa la duración por
        // defecto de la empresa (ajuste appointment.default_duration_minutes) y, en
        // su defecto, 30 minutos.
        @Min(1) @Max(Appointment.MAX_DURATION_MINUTES) Integer durationMinutes,
        @NotNull AppointmentType type, @NotNull Long employeeId, Long animalId, Long ownerId,
        @Size(max = 120) String clientName, @Size(max = 30) String clientPhone,
        // Opcional: correo del contacto libre para enviarle la confirmación.
        @Email @Size(max = 150) String clientEmail, @Size(max = 1000) String notes,
        // Opcional: sede de la cita. Si no se envía, se usa la sede "Principal" de la
        // empresa.
        Long branchId,
        // Opcional (por defecto false): agendar aunque el veterinario ya tenga otra
        // cita cruzada. Sin este flag, el cruce responde 409 APPOINTMENT_OVERLAP.
        // Enviarlo en true exige el permiso appointment.overlap.force además de
        // appointment.create; sin él la respuesta es 403.
        //
        // El @JsonSetter es lo que hace cierto ese "por defecto false": Jackson 3
        // trae FAIL_ON_NULL_FOR_PRIMITIVES ACTIVADO (al reves que Jackson 2), asi
        // que sin la anotacion omitir el campo responde 400 «Cannot map `null`
        // into type `boolean`» y rompe a todo cliente anterior a BE-17.
        @JsonSetter(nulls = Nulls.AS_EMPTY) boolean forceOverlap) {
}
