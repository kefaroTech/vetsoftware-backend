package com.vetsoftware.app.cashregister.application.port.out;

/** Validación mínima de sede para caja: que exista, esté activa y pertenezca a la empresa. */
public interface BranchQueryPort {
    boolean existsActiveInCompany(Long branchId, Long companyId);
}
