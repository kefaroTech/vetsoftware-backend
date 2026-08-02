package com.vetsoftware.app.numberingresolution.application.port.out;

/**
 * Multi-sucursal (B-6): valida la sede de una resolución de sucursal. Una
 * resolución con {@code
 * branchId} debe apuntar a una sede que pertenezca a la propia empresa (evita
 * configurar numeración sobre una sede ajena).
 */
public interface BranchQueryPort {
    boolean existsByIdAndCompanyId(Long branchId, Long companyId);
}
