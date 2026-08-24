package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * El desglose fiscal, que solo se escribe una vez y solo se lee por su
 * documento.
 *
 * <p>
 * <b>Sin ningún {@code UPDATE} ni {@code DELETE}</b>: es append-only. Corregir
 * un desglose es emitir una nota crédito, no reescribir lo declarado.
 */
public interface SubscriptionBillingDocumentTaxJpaRepository
        extends
            JpaRepository<SubscriptionBillingDocumentTaxJpaEntity, Long> {

    /**
     * Las líneas de un documento, acotadas también por empresa.
     *
     * <p>
     * El {@code companyId} es redundante con la FK compuesta
     * {@code fk_sbdt_document}, y va igualmente: el documento se resuelve antes por
     * su carga acotada, así que si alguien llegara aquí con el id de otro tenant no
     * saldría ninguna fila en vez de salir el desglose ajeno.
     */
    List<SubscriptionBillingDocumentTaxJpaEntity> findAllByBillingDocumentIdAndCompanyIdOrderByTaxTreatmentAscTaxRateAsc(
            Long billingDocumentId, Long companyId);

    /**
     * El desglose de <b>toda una página de una empresa</b>, en una sola consulta.
     *
     * <p>
     * Existe para que un listado de veinte documentos no dispare veintiuna
     * consultas. Es el mismo N+1 que {@code @EntityGraph} evita en las asociaciones
     * {@code @ManyToOne}, con la diferencia de que aquí la relación no está mapeada
     * —el desglose es una tabla hija que se lee aparte— y hay que resolverlo a
     * mano.
     *
     * <p>
     * <b>Lleva {@code companyId} por la misma razón que la hermana de arriba, y
     * antes no lo llevaba.</b> Había un único método de lote, sin empresa, que
     * servía tanto al listado del tenant como a los dos barridos de plataforma. No
     * era explotable —los tres orígenes producían ids que el llamador ya tenía
     * derecho a ver— pero aceptaba una {@code Collection<Long>} arbitraria y
     * confiaba en que quien la construyera hubiera filtrado antes. Un cuarto
     * llamador que tomara ids del cliente devolvería base gravable e IVA, factura a
     * factura, de otra clínica. La firma no avisaba de nada; ahora cada uso tiene
     * su método y el nombre dice cuál es cuál.
     */
    List<SubscriptionBillingDocumentTaxJpaEntity> findAllByBillingDocumentIdInAndCompanyIdOrderByTaxTreatmentAscTaxRateAsc(
            Collection<Long> billingDocumentIds, Long companyId);

    /**
     * El desglose de una página <b>que mezcla empresas a propósito</b>: la sirven
     * los dos barridos de plataforma ({@code findAllAwaitingExternal},
     * {@code findAllOverdue}), cuyos casos de uso están cerrados a
     * {@code hasRole('SYSTEM')} a secas.
     *
     * <p>
     * <b>Es cross-tenant y el nombre lo dice, que es justo lo que le faltaba al
     * método único que había antes.</b> Un solo {@code companyId} no vale aquí
     * porque la página trae documentos de varias clínicas; lo que se puede exigir
     * —y es lo que esta forma consigue— es que nadie lo use por accidente creyendo
     * que acota. Quien lo llame tiene que haber escrito {@code AcrossCompanies} en
     * el código y, aguas arriba, un gate de plataforma.
     *
     * <p>
     * Lleva {@code @Query} explícita porque {@code AcrossCompanies} no es una
     * palabra que Spring Data sepa derivar: con la anotación, el nombre puede decir
     * la verdad sin pelearse con el parser de nombres.
     */
    @Query("""
            SELECT t FROM SubscriptionBillingDocumentTaxJpaEntity t
            WHERE t.billingDocumentId IN :billingDocumentIds
            ORDER BY t.taxTreatment ASC, t.taxRate ASC
            """)
    List<SubscriptionBillingDocumentTaxJpaEntity> findAllAcrossCompaniesByBillingDocumentIdIn(
            @Param("billingDocumentIds") Collection<Long> billingDocumentIds);
}
