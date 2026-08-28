package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <strong>Ni un solo {@code UPDATE} ni {@code DELETE}.</strong> La tabla es
 * append-only: corregir es insertar una contra-aplicacion. Que aqui no haya
 * mutaciones SQL no es un descuido, es la propiedad que hace reconstruible el
 * saldo de cualquier factura a partir de sus filas.
 */
public interface BillingDocumentApplicationJpaRepository
        extends
            JpaRepository<BillingDocumentApplicationJpaEntity, Long> {

    @EntityGraph(attributePaths = {"targetDocument", "sourceDocument", "payment", "reversalOf"})
    Optional<BillingDocumentApplicationJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * R13: la busqueda previa que convierte un reintento en la misma respuesta en
     * vez de en un 500 por clave duplicada. Acotada por empresa, que es como esta
     * declarada la unica.
     */
    @EntityGraph(attributePaths = {"targetDocument", "sourceDocument", "payment", "reversalOf"})
    Optional<BillingDocumentApplicationJpaEntity> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId);

    @EntityGraph(attributePaths = {"targetDocument", "sourceDocument", "payment", "reversalOf"})
    Optional<BillingDocumentApplicationJpaEntity> findByReversalOf_IdAndCompanyId(Long reversalOfId,
            Long companyId);

    /**
     * El {@code @EntityGraph} es obligatorio, no cosmetico: las cuatro asociaciones
     * son {@code LAZY} y el mapper las lee todas, asi que sin el son cuatro
     * consultas extra por fila de la pagina.
     */
    @EntityGraph(attributePaths = {"targetDocument", "sourceDocument", "payment", "reversalOf"})
    Page<BillingDocumentApplicationJpaEntity> findAllByTargetDocument_IdAndCompanyId(
            Long targetDocumentId, Long companyId, Pageable pageable);

    /**
     * R3 sobre un pago: suma <strong>neta</strong> de lo aplicado desde el.
     *
     * <p>
     * Neta porque las contra-aplicaciones son negativas
     * ({@code chk_bda_reversal_sign}), asi que revertir una aplicacion libera su
     * importe para volver a aplicarlo. {@code COALESCE} devuelve cero y no
     * {@code null} para que el caso de uso no tenga que distinguir "sin
     * aplicaciones" de "aplicado cero".
     */
    @Query("""
            select coalesce(sum(a.appliedAmount), 0)
            from BillingDocumentApplicationJpaEntity a
            where a.payment.id = :paymentId
              and a.companyId = :companyId
              and a.sourceKind = com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind.PAYMENT
            """)
    BigDecimal sumAppliedFromPayment(@Param("paymentId") Long paymentId,
            @Param("companyId") Long companyId);

    /** R3 sobre una nota credito. Mismo criterio de suma neta. */
    @Query("""
            select coalesce(sum(a.appliedAmount), 0)
            from BillingDocumentApplicationJpaEntity a
            where a.sourceDocument.id = :sourceDocumentId
              and a.companyId = :companyId
              and a.sourceKind = com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind.CREDIT_NOTE
            """)
    BigDecimal sumAppliedFromSourceDocument(@Param("sourceDocumentId") Long sourceDocumentId,
            @Param("companyId") Long companyId);

    /**
     * R3 sobre una retencion. Mismo criterio de suma neta.
     *
     * <p>
     * Filtra tambien por {@code sourceKind} y no solo por la FK: una fila de otro
     * origen no puede llevar {@code withholding_id} relleno
     * ({@code chk_bda_source_exclusive}), asi que el filtro es redundante contra la
     * base y deliberado contra el futuro -- si alguien relajara esa constraint,
     * esta suma seguiria midiendo lo que dice medir.
     */
    @Query("""
            select coalesce(sum(a.appliedAmount), 0)
            from BillingDocumentApplicationJpaEntity a
            where a.withholdingId = :withholdingId
              and a.companyId = :companyId
              and a.sourceKind = com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind.WITHHOLDING
            """)
    BigDecimal sumAppliedFromWithholding(@Param("withholdingId") Long withholdingId,
            @Param("companyId") Long companyId);

    /** R3 sobre un lote de saldo a favor. Mismo criterio de suma neta. */
    @Query("""
            select coalesce(sum(a.appliedAmount), 0)
            from BillingDocumentApplicationJpaEntity a
            where a.creditEntryId = :creditEntryId
              and a.companyId = :companyId
              and a.sourceKind = com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind.CUSTOMER_CREDIT
            """)
    BigDecimal sumAppliedFromCreditEntry(@Param("creditEntryId") Long creditEntryId,
            @Param("companyId") Long companyId);

    /**
     * Facturas que este pago toca. Alimenta el recalculo de {@code settled_amount}
     * cuando el pago cambia de estado: confirmar o devolver un pago mueve el saldo
     * de todas ellas.
     */
    @Query("""
            select distinct a.targetDocument.id
            from BillingDocumentApplicationJpaEntity a
            where a.payment.id = :paymentId and a.companyId = :companyId
            """)
    List<Long> findTargetDocumentIdsByPaymentId(@Param("paymentId") Long paymentId,
            @Param("companyId") Long companyId);
}
