package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.application.dto.AppointmentConfirmationData;

/** Envía al cliente el correo de confirmación cuando se agenda una cita nueva. No debe lanzar. */
public interface AppointmentConfirmationEmailSender {
  void send(AppointmentConfirmationData data);
}
