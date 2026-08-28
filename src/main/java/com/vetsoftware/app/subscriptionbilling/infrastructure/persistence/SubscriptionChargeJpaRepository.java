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
     *
     * <p>
     * <b>El filtro que decide si algo se cobra es {@code charge_mode = 'PAID'} de
     * la línea, y no el estado del contrato</b> ({@code R-TRIAL-13},
     * {@code R-TRIAL-14}). Aquí no se nombra {@code subscriptions.status} y es
     * deliberado: un contrato en {@code TRIALING} factura sus líneas de pago desde
     * el día 0 —la facturación electrónica DIAN se cobra aunque el resto esté en
     * prueba— y descartarlo entero por su estado deja de cobrar un servicio
     * prestado. Al revés, una línea {@code TRIAL} o {@code FREE_LIMITED}
     * <b>conserva su tarifa real</b> para poder convertirla después, así que una
     * consulta que filtre solo por vigencia y periodo no devuelve ceros: devuelve
     * el precio completo y se lo cobra a todos los clientes en prueba.
     *
     * <p>
     * <b>Por qué SQL nativo y por qué {@code LEFT JOIN}.</b> Nativo porque
     * {@code subscription_items} es de otro slice y este adaptador solo necesita
     * una columna suya — mismo criterio que
     * {@link JpaSubscriptionItemValidationPort}, y así la forma interna de la
     * entidad ajena puede cambiar sin arrastrar a la capa de dinero.
     * {@code LEFT JOIN} porque {@code subscription_item_id} es nulo en los cargos
     * que no cuelgan de ninguna línea —implantación, capacitación— y esos sí se
     * cobran: un {@code INNER JOIN} los borraría de la factura en silencio.
     *
     * <p>
     * Un cargo pendiente sobre una línea que no cobra <b>no debería existir</b> —lo
     * impide {@code CreateSubscriptionChargeService}—; si existe, es una fila
     * devengada antes de esa guarda y quedarse fuera de la factura es lo correcto.
     * No desaparece del sistema: sigue {@code PENDING} y visible en el listado, y
     * si era el único del periodo el documento falla con
     * {@code EmptyBillingDocumentException} y su contador {@code NO_CHARGES}.
     */
    @Query(value = """
            SELECT c.*
            FROM subscription_charges c
            LEFT JOIN subscription_items i
                   ON i.id = c.subscription_item_id
                  AND i.company_id = c.company_id
            WHERE c.company_id = :companyId
              AND c.subscription_id = :subscriptionId
              AND c.status = :status
              AND c.service_period_start >= :periodStart
              AND c.service_period_end <= :periodEnd
              AND (c.subscription_item_id IS NULL OR i.charge_mode = 'PAID')
            ORDER BY c.service_period_start ASC, c.id ASC
            """, nativeQuery = true)
    List<SubscriptionChargeJpaEntity> findPendingForPeriod(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId, @Param("status") String status,
            @Param("periodStart") LocalDate periodStart, @Param("periodEnd") LocalDate periodEnd);

    /**
     * Barandilla antiduplicados del barrido recurrente: el cargo de ESA linea para
     * ESE periodo exacto.
     *
     * <p>
     * <b>Sin filtro de estado</b>, porque el caso que cubre es el reinicio despues
     * de emitir: ahi el cargo ya esta {@code INVOICED} y tiene que seguir contando
     * como existente. Y con {@code subscription_item_id} en el {@code WHERE}, que
     * es lo que distingue los dos tramos acumulativos del mismo articulo.
     *
     * <p>
     * <p>
     * <b>Devuelve un conteo y no un booleano proyectado.</b> Un
     * {@code CASE WHEN COUNT(c) > 0 THEN TRUE} se lee mejor y <b>revienta el 100%
     * de las veces</b> con Hibernate 7, que tipa la expresion como {@code Integer}
     * y falla al extraer el {@code Boolean} -es el defecto que ya tuvo la
     * facturacion electronica caida entera, y por el que existe
     * {@code PROYECCION_SIN_LITERAL_BOOLEANO}-. La comparacion con cero la hace el
     * adaptador.
     *
     * <p>
     * Sirve {@code ix_subscription_charges_pending}
     * {@code (company_id, subscription_id, status, service_period_start)} solo en
     * su prefijo; el resto de columnas filtran sobre las filas ya acotadas por
     * contrato, que son pocas.
     */
    @Query("""
            SELECT COUNT(c)
            FROM SubscriptionChargeJpaEntity c
            WHERE c.companyId = :companyId
              AND c.subscriptionId = :subscriptionId
              AND c.subscriptionItemId = :subscriptionItemId
              AND c.chargeType = com.vetsoftware.app.subscriptionbilling.domain.ChargeType.RECURRING
              AND c.servicePeriodStart = :periodStart
              AND c.servicePeriodEnd = :periodEnd
            """)
    long countRecurringCharge(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId,
            @Param("subscriptionItemId") Long subscriptionItemId,
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
     * {@code version = version + 1} — y la regla lo diría. Lo mismo vale para
     * {@link #releaseFromVoidedDocument}, que es su inverso.
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

    /**
     * Libera los cargos que un documento anulado tenía sellados: el inverso de
     * {@link #sealAsInvoiced}.
     *
     * <p>
     * <b>Filtra por documento y no por una lista de ids</b>, y es lo que la hace
     * exhaustiva: quien anula no sabe qué cargos entraron en aquel documento —los
     * selló el ciclo, quizá meses atrás— y una lista construida por el llamador
     * dejaría fuera justo los que nadie recuerda. La FK es la única fuente fiable
     * de esa pertenencia.
     *
     * <p>
     * <b>Nombra la empresa</b> por lo mismo que {@link #sealAsInvoiced}: aquí el
     * {@code WHERE} es toda la seguridad, no hay una lectura previa que valide la
     * propiedad de las filas. Y {@code status = 'INVOICED'} deja intactos los que
     * entre medias se compensaron con un cargo negativo, además de hacer la
     * operación idempotente.
     *
     * <p>
     * {@code billing_document_id = NULL} es obligatorio, no cosmético:
     * {@code chk_subscription_charges_invoiced} exige la referencia mientras el
     * estado sea {@code INVOICED}, y dejarla puesta con el estado {@code PENDING}
     * volvería a atar el cargo a un papel anulado.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE subscription_charges
            SET status = 'PENDING',
                billing_document_id = NULL
            WHERE company_id = :companyId
              AND billing_document_id = :billingDocumentId
              AND status = 'INVOICED'
            """, nativeQuery = true)
    int releaseFromVoidedDocument(@Param("billingDocumentId") Long billingDocumentId,
            @Param("companyId") Long companyId);
}
