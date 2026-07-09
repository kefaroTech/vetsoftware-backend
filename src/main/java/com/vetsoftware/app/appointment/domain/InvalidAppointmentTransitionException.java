package com.vetsoftware.app.appointment.domain;

public class InvalidAppointmentTransitionException extends RuntimeException {
    public InvalidAppointmentTransitionException(AppointmentStatus from, AppointmentStatus to) {
        super("Invalid appointment status transition: " + from + " -> " + to);
    }
}
