package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <b>Ninguna lectura por id sin empresa, y ningún borrado.</b> No se declara
 * {@code findById} propio: el heredado de {@code JpaRepository} existe, pero
 * ningún adaptador de este slice lo usa — la única carga por id del puerto es
 * {@link #findByIdAndCompanyId}.
 */
public interface SubscriptionChargeJpaRepository
        extends
            JpaRepository<SubscriptionChargeJpaEntity, Long> {

    Optional<SubscriptionChargeJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<SubscriptionChargeJpaEntity> findAllByIdInAndCompanyId(Collection<Long> ids,
            Long companyId);

    /**
     * <b>La consulta del proceso de facturación.</b> Sirve
     * {@code ix_subscription_charges_pending}
     * {@code (company_id, subscription_id, status, service_period_start)}.
     *
     * <p>
     * El periodo se compara por <b>contención</b>: solo entran los cargos cuyo
     * periodo de servicio cae entero dentro del periodo que se factura. Un cargo a
     * caballo entre dos periodos no se parte aquí — se devenga ya partido, que es
     * para lo que existen {@code proration_days} y {@code period_days}.
     */
    @Query("""
            SELECT c FROM SubscriptionChargeJpaEntity c
            WHERE c.companyId = :companyId
              AND c.subscriptionId = :subscriptionId
              AND c.status = :status
              AND c.servicePeriodStart >= :periodStart
              AND c.servicePeriodEnd <= :periodEnd
            ORDER BY c.servicePeriodStart ASC, c.id ASC
            """)
    List<SubscriptionChargeJpaEntity> findPendingForPeriod(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId, @Param("status") ChargeStatus status,
            @Param("periodStart") LocalDate periodStart, @Param("periodEnd") LocalDate periodEnd);

    /**
     * Listado del tenant. Los dos filtros opcionales se resuelven con
     * {@code :param IS NULL OR ...}, que es el patrón del árbol, y el
     * {@code companyId} <b>no</b> es opcional: sin él saldrían filas de todas las
     * clínicas.
     */
    @Query("""
            SELECT c FROM SubscriptionChargeJpaEntity c
            WHERE c.companyId = :companyId
              AND (:subscriptionId IS NULL OR c.subscriptionId = :subscriptionId)
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<SubscriptionChargeJpaEntity> findAllByCompany(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId, @Param("status") ChargeStatus status,
            Pageable pageable);

    /**
     * Sella los cargos dentro de su documento.
     *
     * <p>
     * <b>Nombra la empresa, y no es defensa en profundidad: es la defensa.</b> Aquí
     * el {@code WHERE} decide qué filas se sellan, y sin {@code company_id} un id
     * conocido sellaría el cargo de otra clínica dentro de esta factura. También
     * filtra {@code status = 'PENDING'}, de modo que un cargo que dejó de estarlo
     * entre la lectura y el sellado no se toca y el servicio lo detecta contando
     * filas.
     *
     * <p>
     * <b>No mueve ninguna {@code version} y es correcto</b>:
     * {@code subscription_charges} no está versionada ({@code E6_YA_PROTEGIDO}). La
     * regla {@code UPDATE_MASIVO_MUEVE_LA_VERSION} levanta el mapa tabla →
     * ¿versionada? del censo de {@code @Entity}, así que añadirle un
     * {@code @Version} a esa entidad haría que esta consulta pasara a necesitar
     * {@code version = version + 1} — y la regla lo diría.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE subscription_charges
            SET status = 'INVOICED',
                billing_document_id = :billingDocumentId
            WHERE company_id = :companyId
              AND id IN (:ids)
              AND status = 'PENDING'
            """, nativeQuery = true)
    int sealAsInvoiced(@Param("ids") Collection<Long> ids, @Param("companyId") Long companyId,
            @Param("billingDocumentId") Long billingDocumentId);
}
