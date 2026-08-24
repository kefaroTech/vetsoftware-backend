package com.vetsoftware.app.cashterminal.application.port.out;

/** Reserva y libera puestos TERMINAL del contrato de la empresa. */
public interface CashTerminalCapacityPort {
    void reserve(Long companyId);

    void release(Long companyId);
}
