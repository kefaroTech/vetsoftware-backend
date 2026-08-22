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

public record CreateAppointmentRequest(
        @NotNull(message = "La fecha y hora de la cita son obligatorias.") LocalDateTime startAt,
        // Opcional: duración en minutos. Si no se envía, se usa la duración por
        // defecto de la empresa (ajuste appointment.default_duration_minutes) y, en
        // su defecto, 30 minutos.
        @Min(value = 1, message = "La duración debe ser de al menos 1 minuto.") @Max(value = Appointment.MAX_DURATION_MINUTES, message = "La duración no puede superar los "
                + Appointment.MAX_DURATION_MINUTES + " minutos.") Integer durationMinutes,
        @NotNull(message = "Debes seleccionar el tipo de cita.") AppointmentType type,
        @NotNull(message = "Debes seleccionar el veterinario que atiende.") Long employeeId,
        Long animalId, Long ownerId,
        @Size(max = 120, message = "El nombre del cliente no puede superar los 120 caracteres.") String clientName,
        @Size(max = 30, message = "El teléfono del cliente no puede superar los 30 caracteres.") String clientPhone,
        // Opcional: correo del contacto libre para enviarle la confirmación.
        @Email(message = "El correo electrónico no tiene un formato válido.") @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres.") String clientEmail,
        @Size(max = 1000, message = "Las notas no pueden superar los 1000 caracteres.") String notes,
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
