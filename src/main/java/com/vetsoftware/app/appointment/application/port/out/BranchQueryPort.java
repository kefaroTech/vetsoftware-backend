package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.domain.BranchRef;
import java.util.Optional;

/**
 * Resuelve la sucursal de una cita. Si el request trae {@code branchId}, debe pertenecer a la empresa y estar
 * ACTIVA (no se agenda en una sede fuera de operación); si no, se usa la sede ACTIVA por defecto ("Principal"
 * activa, o la primera activa) — así las empresas de una sola sede funcionan sin enviar branchId, y quedan
 * listas para el selector de sede (Fase C).
 */
public interface BranchQueryPort {
    /** Sucursal ACTIVA que pertenece a la empresa. Vacío si no existe o está inactiva. */
    Optional<BranchRef> findActiveByIdAndCompanyId(Long branchId, Long companyId);

    /** ¿Existe la sucursal en la empresa (activa o no)? Distingue "inactiva" de "inexistente". */
    boolean existsByIdAndCompanyId(Long branchId, Long companyId);

    /** Sede ACTIVA por defecto: la "Principal" activa, o la primera activa. Vacío si no hay ninguna activa. */
    Optional<BranchRef> findDefaultActiveByCompanyId(Long companyId);

    /** Dirección de la sucursal (para el correo de confirmación). Vacío si no existe o no tiene dirección. */
    Optional<String> findAddressById(Long branchId);
}
