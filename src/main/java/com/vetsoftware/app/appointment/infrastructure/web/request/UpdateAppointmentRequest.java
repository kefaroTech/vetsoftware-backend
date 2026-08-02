package com.vetsoftware.app.appointment.infrastructure.web.request;

import com.vetsoftware.app.appointment.domain.AppointmentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdateAppointmentRequest(@NotNull LocalDateTime startAt,
        @NotNull AppointmentType type, @NotNull Long employeeId, Long animalId, Long ownerId,
        @Size(max = 120) String clientName, @Size(max = 30) String clientPhone,
        // Opcional: correo del contacto libre para enviarle la confirmación.
        @Email @Size(max = 150) String clientEmail, @Size(max = 1000) String notes) {
}
