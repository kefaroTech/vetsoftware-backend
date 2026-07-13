package com.vetsoftware.app.appointment.application.dto;

import com.vetsoftware.app.appointment.domain.AppointmentType;
import java.time.LocalDateTime;

/**
 * Datos ya resueltos para el correo de confirmación de cita. {@code petName}, {@code branchAddress} y
 * {@code notes} pueden ser {@code null} (el adaptador aplica un valor por defecto legible).
 */
public record AppointmentConfirmationData(
        String recipientEmail,
        String recipientName,
        String companyName,
        LocalDateTime startAt,
        AppointmentType type,
        String vetName,
        String petName,
        String branchName,
        String branchAddress,
        String notes
) {}
