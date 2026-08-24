package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.TaxBreakdown;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Las tres lecturas del desglose fiscal, y sobre todo la que mezcla empresas a
 * proposito.
 *
 * <p>
 * <b>{@code findAllAcrossCompaniesByBillingDocumentIdIn} es la unica de las
 * tres que lleva {@code @Query} escrita a mano</b>, porque
 * {@code AcrossCompanies} no es una palabra que el parser de nombres de Spring
 * Data sepa derivar. Eso la saca del unico chequeo automatico que tienen las
 * consultas derivadas —Spring Data valida el nombre contra la entidad al
 * arrancar— y la deja dependiendo de que alguien la ejecute. Nadie lo habia
 * hecho: no hay ninguna rodaja que la llame, y su unico llamador de produccion
 * son los dos barridos de plataforma, que solo corren bajo
 * {@code hasRole('SYSTEM')}. Un JPQL con un nombre de propiedad equivocado
 * arranca el contexto igual y revienta en la primera invocacion.
 *
 * <p>
 * <b>La pareja de casos que importa</b> es la que enfrenta la variante
 * cross-tenant con su hermana acotada sobre exactamente los mismos ids: la
 * primera tiene que traer el desglose de las dos clinicas, y la segunda solo el
 * de la suya. Si alguien "arreglara" la cross-tenant metiendole un
 * {@code company_id}, los barridos de plataforma devolverian paginas mudas; si
 * a la acotada se le cayera el filtro, un listado de tenant entregaria base
 * gravable e IVA factura a factura de otra clinica.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("SubscriptionBillingDocumentTaxJpaRepository — desglose fiscal contra MySQL real")
class BillingDocumentTaxQueryPersistenceIT extends AbstractDataJpaTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-01T10:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(CLOCK);
    private static final ServicePeriod PERIODO = new ServicePeriod(LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28));

    @Autowired
    private JpaBillingDocumentRepository documentRepository;
    @Autowired
    private SubscriptionBillingDocumentTaxJpaRepository taxRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private Long documentoPropioId;
    private Long documentoAjenoId;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        documentoPropioId = emitir("DCT-A", SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                SchemaSeed.SUBSCRIPTION_ITEM_ID);
        documentoAjenoId = emitir("DCT-B", SchemaSeed.OTRA_COMPANY_ID,
                SchemaSeed.OTRA_SUBSCRIPTION_ID, SchemaSeed.OTRO_SUBSCRIPTION_ITEM_ID);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("La variante que mezcla empresas")
    class CrossTenant {

        @Test
        @DisplayName("trae el desglose de las dos clinicas cuando la pagina las mezcla:"
                + " es lo que sirve a los barridos de plataforma")
        void trae_el_desglose_de_las_dos_clinicas() {
            List<SubscriptionBillingDocumentTaxJpaEntity> desglose = taxRepository
                    .findAllAcrossCompaniesByBillingDocumentIdIn(
                            List.of(documentoPropioId, documentoAjenoId));

            assertThat(desglose)
                    .extracting(SubscriptionBillingDocumentTaxJpaEntity::getBillingDocumentId)
                    .contains(documentoPropioId, documentoAjenoId);
            assertThat(desglose).extracting(SubscriptionBillingDocumentTaxJpaEntity::getCompanyId)
                    .containsOnly(SchemaSeed.COMPANY_ID, SchemaSeed.OTRA_COMPANY_ID);
        }

        @Test
        @DisplayName("una lista vacia no trae el desglose de todo el mundo: trae nada")
        void una_lista_vacia_trae_nada() {
            assertThat(taxRepository.findAllAcrossCompaniesByBillingDocumentIdIn(List.of()))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Las variantes acotadas por empresa")
    class AcotadasPorEmpresa {

        @Test
        @DisplayName("sobre los MISMOS ids que la cross-tenant, el lote acotado deja fuera"
                + " el desglose de la otra clinica")
        void el_lote_acotado_deja_fuera_lo_ajeno() {
            List<SubscriptionBillingDocumentTaxJpaEntity> acotado = taxRepository
                    .findAllByBillingDocumentIdInAndCompanyIdOrderByTaxTreatmentAscTaxRateAsc(
                            List.of(documentoPropioId, documentoAjenoId), SchemaSeed.COMPANY_ID);

            assertThat(acotado).isNotEmpty()
                    .allSatisfy(
                            tax -> assertThat(tax.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID))
                    .extracting(SubscriptionBillingDocumentTaxJpaEntity::getBillingDocumentId)
                    .containsOnly(documentoPropioId);
        }

        @Test
        @DisplayName("el desglose de un documento ajeno pedido con la empresa propia sale"
                + " vacio, no sale el ajeno")
        void el_documento_ajeno_pedido_con_la_empresa_propia_sale_vacio() {
            assertThat(taxRepository
                    .findAllByBillingDocumentIdAndCompanyIdOrderByTaxTreatmentAscTaxRateAsc(
                            documentoAjenoId, SchemaSeed.COMPANY_ID))
                    .isEmpty();

            assertThat(taxRepository
                    .findAllByBillingDocumentIdAndCompanyIdOrderByTaxTreatmentAscTaxRateAsc(
                            documentoAjenoId, SchemaSeed.OTRA_COMPANY_ID))
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Orden del desglose")
    class Orden {

        @Test
        @DisplayName("un documento con tres bloques sale ordenado de forma estable por"
                + " tratamiento y despues por tarifa")
        void sale_ordenado_de_forma_estable() {
            List<SubscriptionBillingDocumentTaxJpaEntity> desglose = taxRepository
                    .findAllByBillingDocumentIdAndCompanyIdOrderByTaxTreatmentAscTaxRateAsc(
                            documentoPropioId, SchemaSeed.COMPANY_ID);

            assertThat(desglose).hasSize(3);
            assertThat(desglose)
                    .extracting(SubscriptionBillingDocumentTaxJpaEntity::getTaxTreatment)
                    .containsExactly(TaxTreatment.EXCLUDED, TaxTreatment.TAXED, TaxTreatment.TAXED);
            assertThat(desglose.get(1).getTaxRate()).isEqualByComparingTo("5.00");
            assertThat(desglose.get(2).getTaxRate()).isEqualByComparingTo("19.00");
        }

        @Test
        @DisplayName("la variante cross-tenant ordena con el mismo criterio dentro de"
                + " cada documento")
        void la_cross_tenant_ordena_con_el_mismo_criterio() {
            List<SubscriptionBillingDocumentTaxJpaEntity> desglose = taxRepository
                    .findAllAcrossCompaniesByBillingDocumentIdIn(List.of(documentoPropioId));

            assertThat(desglose)
                    .extracting(SubscriptionBillingDocumentTaxJpaEntity::getTaxTreatment)
                    .containsExactly(TaxTreatment.EXCLUDED, TaxTreatment.TAXED, TaxTreatment.TAXED);
        }
    }

    /**
     * Un documento {@code ONE_TIME} con tres bloques fiscales: dos tarifas gravadas
     * distintas y una excluida. {@code ONE_TIME} y no {@code RECURRING_CYCLE} para
     * que {@code recurring_cycle_marker} quede nulo y las dos empresas puedan
     * compartir periodo sin chocar con {@code uq_sbd_recurring_cycle}.
     */
    private Long emitir(String prefijo, Long companyId, Long subscriptionId, Long itemId) {
        List<SubscriptionCharge> cargos = List.of(
                cargo(companyId, subscriptionId, itemId, "Cuota gravada al 19",
                        new BigDecimal("100000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED),
                cargo(companyId, subscriptionId, itemId, "Servicio gravado al 5",
                        new BigDecimal("50000.00"), new BigDecimal("5.00"), TaxTreatment.TAXED),
                cargo(companyId, subscriptionId, itemId, "Concepto excluido",
                        new BigDecimal("30000.00"), BigDecimal.ZERO, TaxTreatment.EXCLUDED));
        TaxBreakdown desglose = TaxBreakdown.of(cargos, DocumentKind.INVOICE, companyId, AHORA);
        SubscriptionBillingDocument documento = documentRepository
                .save(SubscriptionBillingDocument.issue(new DocumentNumber(prefijo, 1), companyId,
                        subscriptionId, DocumentKind.INVOICE, BillingReason.ONE_TIME, PERIODO,
                        desglose, null, CLOCK));
        return documento.getId();
    }

    private SubscriptionCharge cargo(Long companyId, Long subscriptionId, Long itemId,
            String descripcion, BigDecimal subtotal, BigDecimal tarifa, TaxTreatment tratamiento) {
        return SubscriptionCharge.create(companyId, subscriptionId, itemId, ChargeType.ONE_TIME,
                descripcion, PERIODO, BigDecimal.ONE, subtotal, subtotal, tarifa, tratamiento, null,
                null, CLOCK);
    }
}
