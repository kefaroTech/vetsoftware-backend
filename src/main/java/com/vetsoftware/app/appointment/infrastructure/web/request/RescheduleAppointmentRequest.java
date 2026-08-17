package com.vetsoftware.app.appointment.infrastructure.web.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.vetsoftware.app.appointment.domain.Appointment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(@NotNull LocalDateTime startAt,
        // Opcional: nueva duración en minutos. Es un PATCH, así que omitirla
        // CONSERVA la duración que la cita ya tenía (no vuelve al valor por defecto).
        @Min(1) @Max(Appointment.MAX_DURATION_MINUTES) Integer durationMinutes,
        @NotNull Long employeeId,
        // Opcional (por defecto false): reprogramar aunque el veterinario ya tenga
        // otra cita cruzada. Sin este flag, el cruce responde 409
        // APPOINTMENT_OVERLAP. Enviarlo en true exige el permiso
        // appointment.overlap.force además de appointment.update; sin él, 403.
        //
        // El @JsonSetter es lo que hace cierto ese "por defecto false": Jackson 3
        // trae FAIL_ON_NULL_FOR_PRIMITIVES ACTIVADO (al reves que Jackson 2), asi
        // que sin la anotacion omitir el campo responde 400 «Cannot map `null`
        // into type `boolean`» y rompe a todo cliente anterior a BE-17.
        @JsonSetter(nulls = Nulls.AS_EMPTY) boolean forceOverlap) {
}
