package com.vetsoftware.app.pricelist.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La comprobacion de cobertura de tramos, que el documento de modelo elogia
 * como excelente y que <b>no tenia un solo test</b>.
 *
 * <p>
 * Dos mitades distintas y dos defectos distintos:
 *
 * <ul>
 * <li><b>Continuidad</b> (incidencia #378): los tramos de un
 * {@code (articulo, ciclo)} arrancan en 1, encadenan sin salto y el ultimo es
 * abierto.
 * <li><b>Cobertura contra los articulos ACTIVOS</b> (R-PRICE-05, defecto
 * construido #16): la continuidad agrupa sobre los precios ESCRITOS, asi que un
 * articulo sin ninguna fila no producia grupo, no producia hueco y la lista se
 * publicaba limpia. Si el olvidado es el nucleo, ninguna empresa puede
 * registrarse.
 * </ul>
 */
@DisplayName("PriceListTierCoverage — cobertura de tramos al publicar")
class PriceListTierCoverageTest {

    private static final Long LISTA = 5L;
    private static final Long NUCLEO = 100L;
    private static final Long USUARIO_EXTRA = 200L;
    private static final LocalDateTime AYER = LocalDateTime.of(2026, 3, 1, 10, 0);

    @Nested
    @DisplayName("R-PRICE-05 — contra los articulos activos")
    class ContraLosArticulosActivos {

        @Test
        @DisplayName("publicar una lista que olvida el precio del articulo nucleo es rechazado — "
                + "hoy pasa limpia y ninguna empresa puede registrarse")
        void publicar_una_lista_que_olvida_el_precio_del_articulo_nucleo_es_rechazado() {
            // La lista tarifa el usuario extra perfectamente y se olvida del nucleo.
            List<CatalogPrice> soloElUsuarioExtra = List.of(tramo(USUARIO_EXTRA, 1, null, 0));

            assertThatThrownBy(() -> PriceListTierCoverage.requireFullCoverage(LISTA,
                    soloElUsuarioExtra, List.of(NUCLEO, USUARIO_EXTRA)))
                    .isInstanceOf(CatalogPriceMissingForActiveItemException.class)
                    .hasMessageContaining("active catalog item " + NUCLEO)
                    .hasMessageContaining("has no price");
        }

        @Test
        @DisplayName("la continuidad sola NO lo detecta: sin articulos activos que contrastar, la "
                + "misma lista pasa limpia")
        void la_continuidad_sola_no_lo_detecta() {
            // Es exactamente el defecto: agrupar sobre lo escrito no ve una ausencia total.
            assertThatCode(() -> PriceListTierCoverage.requireFullCoverage(LISTA,
                    List.of(tramo(USUARIO_EXTRA, 1, null, 0)), List.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("con todos los articulos activos tarifados, publicar pasa")
        void con_todos_los_articulos_activos_tarifados_publicar_pasa() {
            assertThatCode(() -> PriceListTierCoverage.requireFullCoverage(LISTA,
                    List.of(tramo(NUCLEO, 1, null, 2), tramo(USUARIO_EXTRA, 1, 8, 0),
                            tramo(USUARIO_EXTRA, 9, null, 0)),
                    List.of(NUCLEO, USUARIO_EXTRA))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("señala siempre el mismo articulo: el de id mas bajo de los que faltan")
        void senala_siempre_el_mismo_articulo() {
            assertThatThrownBy(() -> PriceListTierCoverage.requireFullCoverage(LISTA, List.of(),
                    List.of(USUARIO_EXTRA, NUCLEO)))
                    .isInstanceOf(CatalogPriceMissingForActiveItemException.class)
                    .hasMessageContaining(String.valueOf(NUCLEO));
        }

        @Test
        @DisplayName("un articulo tarifado solo en un ciclo cuenta como tarifado: la ausencia que "
                + "esta regla persigue es la TOTAL")
        void un_articulo_tarifado_solo_en_un_ciclo_cuenta_como_tarifado() {
            assertThatCode(() -> PriceListTierCoverage.requireFullCoverage(LISTA,
                    List.of(tramo(NUCLEO, 1, null, 0, BillingCycle.ANNUAL)), List.of(NUCLEO)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Continuidad de los tramos (#378)")
    class Continuidad {

        @Test
        @DisplayName("un hueco entre el tramo 1-10 y el 21 en adelante se rechaza nombrando el "
                + "rango que falta")
        void un_hueco_entre_tramos_se_rechaza() {
            assertThatThrownBy(
                    () -> PriceListTierCoverage.requireFullCoverage(LISTA,
                            List.of(tramo(USUARIO_EXTRA, 1, 10, 0),
                                    tramo(USUARIO_EXTRA, 21, null, 0)),
                            List.of(USUARIO_EXTRA)))
                    .isInstanceOf(CatalogPriceTierGapException.class)
                    .hasMessageContaining("quantities 11 to 20");
        }

        @Test
        @DisplayName("una escalera que no arranca en 1 deja sin precio las primeras unidades")
        void una_escalera_que_no_arranca_en_1_se_rechaza() {
            assertThatThrownBy(() -> PriceListTierCoverage.requireFullCoverage(LISTA,
                    List.of(tramo(USUARIO_EXTRA, 3, null, 0)), List.of(USUARIO_EXTRA)))
                    .isInstanceOf(CatalogPriceTierGapException.class)
                    .hasMessageContaining("quantities 1 to 2");
        }

        @Test
        @DisplayName("un ultimo tramo cerrado deja sin precio todo lo que hay por encima")
        void un_ultimo_tramo_cerrado_se_rechaza() {
            assertThatThrownBy(() -> PriceListTierCoverage.requireFullCoverage(LISTA,
                    List.of(tramo(USUARIO_EXTRA, 1, 10, 0)), List.of(USUARIO_EXTRA)))
                    .isInstanceOf(CatalogPriceTierGapException.class)
                    .hasMessageContaining("quantities 11 and above");
        }

        @Test
        @DisplayName("los ciclos son grupos distintos: un mensual completo no tapa un anual roto")
        void los_ciclos_son_grupos_distintos() {
            assertThatThrownBy(() -> PriceListTierCoverage.requireFullCoverage(LISTA,
                    List.of(tramo(USUARIO_EXTRA, 1, null, 0),
                            tramo(USUARIO_EXTRA, 5, null, 0, BillingCycle.ANNUAL)),
                    List.of(USUARIO_EXTRA))).isInstanceOf(CatalogPriceTierGapException.class)
                    .hasMessageContaining("ANNUAL");
        }

        @Test
        @DisplayName("una escalera continua y abierta por arriba pasa")
        void una_escalera_continua_y_abierta_pasa() {
            assertThatCode(
                    () -> PriceListTierCoverage.requireFullCoverage(LISTA,
                            List.of(tramo(USUARIO_EXTRA, 1, 8, 2),
                                    tramo(USUARIO_EXTRA, 9, null, 0)),
                            List.of(USUARIO_EXTRA)))
                    .doesNotThrowAnyException();
        }
    }

    private static CatalogPrice tramo(Long catalogItemId, int tierMin, Integer tierMax,
            int included) {
        return tramo(catalogItemId, tierMin, tierMax, included, BillingCycle.MONTHLY);
    }

    private static CatalogPrice tramo(Long catalogItemId, int tierMin, Integer tierMax,
            int included, BillingCycle cycle) {
        return CatalogPrice.create(LISTA, catalogItemId, cycle, tierMin, tierMax, included,
                new BigDecimal("12000.00"), BigDecimal.ZERO, new BigDecimal("19.00"),
                TaxTreatment.TAXED, AYER);
    }
}
