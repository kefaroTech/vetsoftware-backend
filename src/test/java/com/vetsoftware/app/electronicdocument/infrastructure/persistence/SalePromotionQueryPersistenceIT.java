package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.ApplicationType;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.PromotionType;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.SalePromotion;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.ValueType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del catalogo de promociones que consume el POS.
 *
 * <p>
 * Este es el unico sitio del backend que decide <b>que promocion esta vigente
 * al cobrar</b>. El precio que el servidor recalcula para validar cada linea
 * sale de aqui: una promocion caducada que siguiera colandose descontaria
 * dinero real en caja, y una vigente que se perdiera cobraria de mas al
 * cliente.
 *
 * <p>
 * <b>Por que contra la base y no con un doble.</b> El filtro tiene dos mitades
 * y solo una esta en Java. La vigencia y el estado se calculan en el stream del
 * adaptador; el {@code enabled} lo aplica MySQL a traves del
 * {@code @SQLRestriction} de {@code PromotionJpaEntity}, que ningun doble
 * reproduce. Un repositorio en memoria daria verde con promociones borradas
 * dentro del carrito.
 *
 * <p>
 * Las promociones se insertan por SQL nativo, no por el adaptador de la feature
 * {@code promotion}: el vertical slicing tambien aplica en {@code src/test}, y
 * asi la rodaja no depende de que el otro adaptador este bien.
 */
@Import(JpaSalePromotionQueryPort.class)
@DisplayName("JpaSalePromotionQueryPort — promociones vigentes contra MySQL real")
class SalePromotionQueryPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    private static final LocalDate HOY = LocalDate.of(2026, 1, 15);
    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    private static final Long ITEM_VIGENTE = 1L;
    private static final Long ITEM_CADUCADA = 2L;
    private static final Long ITEM_FUTURA = 3L;

    @Autowired
    private JpaSalePromotionQueryPort port;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private void promocion(String nombre, Long applicationItem, LocalDate desde, LocalDate hasta,
            String estado, Long companyId, boolean habilitada) {
        entityManager.createNativeQuery("""
                INSERT INTO promotions (name, promotion_type, application_type, application_item,
                                        value_type, `value`, start_date, end_date,
                                        promotion_status, company_id, created_date, enabled)
                VALUES (:name, 'DISCOUNT', 'CATEGORY', :item, 'PERCENTAGE', 15.00,
                        :desde, :hasta, :estado, :company, '2025-12-20 09:00:00', :habilitada)
                """).setParameter("name", nombre).setParameter("item", applicationItem)
                .setParameter("desde", desde.atStartOfDay())
                .setParameter("hasta", hasta.atTime(23, 59)).setParameter("estado", estado)
                .setParameter("company", companyId)
                // 1/0 en vez de boolean: la columna es TINYINT pelado y el binding
                // explicito evita depender de como infiera el tipo la query nativa.
                .setParameter("habilitada", habilitada ? 1 : 0).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private void vigente() {
        promocion("Enero perruno", ITEM_VIGENTE, DESDE, HASTA, "ACTIVE", COMPANY, true);
    }

    @Nested
    @DisplayName("vigencia por fechas")
    class Vigencia {

        @Test
        @DisplayName("una promocion dentro del rango se selecciona")
        void dentro_del_rango_se_selecciona() {
            vigente();

            assertThat(port.findActive(COMPANY, HOY)).extracting(SalePromotion::applicationItem)
                    .containsExactly(ITEM_VIGENTE);
        }

        @Test
        @DisplayName("una promocion caducada no se selecciona")
        void una_promocion_caducada_no_se_selecciona() {
            promocion("Diciembre gatuno", ITEM_CADUCADA, LocalDate.of(2025, 12, 1),
                    LocalDate.of(2025, 12, 31), "ACTIVE", COMPANY, true);

            // Caducada el 31/12 y estamos a 15/01: si se colara, el POS descontaria
            // dinero real por una promocion que ya no existe comercialmente.
            assertThat(port.findActive(COMPANY, HOY)).isEmpty();
        }

        @Test
        @DisplayName("una promocion que aun no empieza no se selecciona")
        void una_promocion_futura_no_se_selecciona() {
            promocion("Febrero felino", ITEM_FUTURA, LocalDate.of(2026, 2, 1),
                    LocalDate.of(2026, 2, 28), "ACTIVE", COMPANY, true);

            assertThat(port.findActive(COMPANY, HOY)).isEmpty();
        }

        @Test
        @DisplayName("el primer y el ultimo dia del rango cuentan como vigentes")
        void los_bordes_del_rango_son_inclusivos() {
            vigente();

            // Los bordes son el sitio clasico del off-by-one: el ultimo dia de la
            // promocion es el dia de mas venta.
            assertThat(port.findActive(COMPANY, DESDE)).hasSize(1);
            assertThat(port.findActive(COMPANY, HASTA)).hasSize(1);
            assertThat(port.findActive(COMPANY, DESDE.minusDays(1))).isEmpty();
            assertThat(port.findActive(COMPANY, HASTA.plusDays(1))).isEmpty();
        }
    }

    @Nested
    @DisplayName("estado y borrado logico")
    class EstadoYBorrado {

        @Test
        @DisplayName("una promocion INACTIVE no se selecciona aunque este en fecha")
        void una_promocion_inactive_no_se_selecciona() {
            promocion("Pausada", ITEM_VIGENTE, DESDE, HASTA, "INACTIVE", COMPANY, true);

            assertThat(port.findActive(COMPANY, HOY)).isEmpty();
        }

        @Test
        @DisplayName("una promocion borrada logicamente no llega ni a salir de la base")
        void una_promocion_deshabilitada_no_se_selecciona() {
            promocion("Borrada", ITEM_VIGENTE, DESDE, HASTA, "ACTIVE", COMPANY, false);

            // Este filtro no esta en el Java del adaptador: lo aplica MySQL por el
            // @SQLRestriction de la entidad. Es justo el que un doble se salta.
            assertThat(port.findActive(COMPANY, HOY)).isEmpty();
        }

        @Test
        @DisplayName("convive lo vigente con lo caducado y solo devuelve lo primero")
        void convive_lo_vigente_con_lo_caducado() {
            vigente();
            promocion("Diciembre gatuno", ITEM_CADUCADA, LocalDate.of(2025, 12, 1),
                    LocalDate.of(2025, 12, 31), "ACTIVE", COMPANY, true);
            promocion("Febrero felino", ITEM_FUTURA, LocalDate.of(2026, 2, 1),
                    LocalDate.of(2026, 2, 28), "ACTIVE", COMPANY, true);

            assertThat(port.findActive(COMPANY, HOY)).extracting(SalePromotion::applicationItem)
                    .containsExactly(ITEM_VIGENTE);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa y forma de salida")
    class TenancyYMapeo {

        @Test
        @DisplayName("la promocion vigente de otra empresa no entra en el carrito")
        void una_promocion_de_otra_empresa_no_entra() {
            promocion("Promo ajena", ITEM_VIGENTE, DESDE, HASTA, "ACTIVE", OTRA_COMPANY, true);

            assertThat(port.findActive(COMPANY, HOY)).isEmpty();
            assertThat(port.findActive(OTRA_COMPANY, HOY)).hasSize(1);
        }

        @Test
        @DisplayName("la salida traduce los enums a la forma local sin acoplar la otra feature")
        void la_salida_usa_la_forma_local() {
            vigente();

            assertThat(port.findActive(COMPANY, HOY)).singleElement().satisfies(promo -> {
                assertThat(promo.promotionType()).isEqualTo(PromotionType.DISCOUNT);
                assertThat(promo.applicationType()).isEqualTo(ApplicationType.CATEGORY);
                assertThat(promo.valueType()).isEqualTo(ValueType.PERCENTAGE);
                assertThat(promo.value()).isEqualByComparingTo("15.00");
            });
        }
    }
}
