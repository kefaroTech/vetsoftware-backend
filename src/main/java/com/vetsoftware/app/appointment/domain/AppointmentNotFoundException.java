package com.vetsoftware.app.appointment.domain;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(Long id) {
        super("Appointment not found: " + id);
    }
}
