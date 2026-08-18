package com.vetsoftware.app.productchargeopenaccount.application.port.out;

import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import java.math.BigDecimal;
import java.util.Optional;

public interface OpenAccountQueryPort {
    /**
     * Variante ancha, reservada al camino SYSTEM (principal sin empresa). Ningun
     * caso de uso de tenant debe usarla: usa
     * {@link #findByIdAndCompanyId(Long, Long)}.
     */
    Optional<OpenAccountRef> findById(Long openAccountId);

    /**
     * Resuelve la cuenta SOLO si pertenece a la empresa indicada.
     *
     * <p>
     * Es LA barrera de aislamiento, no defensa en profundidad: cargar con
     * {@link #findById(Long)} y comparar la empresa despues en Java deja el cargo
     * colgado de la cuenta de otro tenant en cuanto ese {@code if} se mueva o se
     * copie sin el (BE-COV,
     * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}).
     */
    Optional<OpenAccountRef> findByIdAndCompanyId(Long openAccountId, Long companyId);

    /**
     * Toma el bloqueo pesimista (FOR UPDATE) sobre la cuenta al inicio de la
     * operación, para serializar cargos/abonos concurrentes desde el
     * read-modify-write completo (validación de estado + recálculo) y no solo
     * durante el recálculo. Debe invocarse como primera sentencia del caso de uso.
     * No-op si la cuenta no existe (la validación posterior lo reporta).
     *
     * <p>
     * Va ACOTADO por empresa por el mismo motivo que
     * {@link #findByIdAndCompanyId(Long, Long)}: sin el {@code companyId} tomaba un
     * {@code PESSIMISTIC_WRITE} sobre la fila de otro tenant antes de cualquier
     * comprobacion —lo soltaba el rollback de la operacion rechazada, pero se
     * concedia—. Con la variante acotada la cuenta ajena no devuelve fila y no se
     * bloquea nada.
     */
    void lockForUpdate(Long openAccountId, Long companyId);

    boolean isOpen(Long openAccountId);

    /** Saldo pendiente actual de la cuenta (total - abonos). ZERO si no existe. */
    BigDecimal outstandingAmount(Long openAccountId);
}
