package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.domain.AppointmentStatus;

/** Telemetría de transiciones reales del ciclo de vida de una cita. */
public interface AppointmentMetrics {

    void transitioned(AppointmentStatus status, Channel channel);

    enum Channel {
        STAFF("staff"), PUBLIC("public");

        private final String value;

        Channel(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
