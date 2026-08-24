package com.vetsoftware.app.branch.application.port.out;

/** Reserva y libera puestos BRANCH del contrato de la empresa. */
public interface BranchCapacityPort {
    void reserve(Long companyId);

    void release(Long companyId);
}
