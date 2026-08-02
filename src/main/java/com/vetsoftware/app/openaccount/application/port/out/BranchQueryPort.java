package com.vetsoftware.app.openaccount.application.port.out;

import com.vetsoftware.app.openaccount.domain.BranchRef;
import java.util.Optional;

/**
 * Resuelve la sucursal de una cuenta abierta. Si el request trae
 * {@code branchId} se valida contra la empresa; si no, se usa la sede por
 * defecto ("Principal"). Empresas de una sola sede funcionan sin enviarlo.
 */
public interface BranchQueryPort {
    /**
     * Sucursal ACTIVA que pertenece a la empresa. Vacío si no existe o está
     * inactiva.
     */
    Optional<BranchRef> findActiveByIdAndCompanyId(Long branchId, Long companyId);

    /**
     * ¿Existe la sucursal en la empresa (activa o no)? Distingue "inactiva" de
     * "inexistente".
     */
    boolean existsByIdAndCompanyId(Long branchId, Long companyId);

    /**
     * Sede ACTIVA por defecto: la "Principal" activa, o la primera activa. Vacío si
     * no hay ninguna activa.
     */
    Optional<BranchRef> findDefaultActiveByCompanyId(Long companyId);
}
