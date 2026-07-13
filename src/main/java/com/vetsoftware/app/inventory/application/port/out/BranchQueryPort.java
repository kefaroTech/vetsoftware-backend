package com.vetsoftware.app.inventory.application.port.out;

/** Validación mínima de sede para inventario: que exista, esté activa y pertenezca a la empresa. */
public interface BranchQueryPort {
    boolean existsActiveInCompany(Long branchId, Long companyId);
}
