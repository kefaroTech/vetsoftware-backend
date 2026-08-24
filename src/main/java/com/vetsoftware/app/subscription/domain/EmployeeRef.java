package com.vetsoftware.app.subscription.domain;

/**
 * Companion VO del empleado que firma un otrosi. Este slice no importa el
 * dominio de {@code employee}: se resuelve por
 * {@code EmployeeQueryPort.findByIdAndCompanyId}, que es lo que hace cumplir
 * R14 —<em>el empleado que firma es de la misma empresa que el contrato</em>—,
 * porque {@code fk_subscription_amendments_employee} es una FK simple y la base
 * no puede imponerlo.
 */
public record EmployeeRef(Long id, String name) {
    public EmployeeRef {
        if (id == null)
            throw new IllegalArgumentException("employee id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("employee name is required");
    }
}
