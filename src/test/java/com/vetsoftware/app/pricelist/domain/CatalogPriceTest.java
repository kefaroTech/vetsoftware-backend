package com.vetsoftware.app.pricelist.domain;

import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.ARTICULO;
import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.CREADO_EL;
import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.LISTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CatalogPrice — el precio de un artículo dentro de una lista")
class CatalogPriceTest {

    private static CatalogPrice conImportes(BigDecimal unit, BigDecimal setup) {
        return CatalogPrice.create(LISTA, ARTICULO, BillingCycle.MONTHLY, 1, null, 0, unit, setup,
                new BigDecimal("19.00"), TaxTreatment.TAXED, CREADO_EL);
    }

    private static CatalogPrice conFiscalidad(BigDecimal rate, TaxTreatment treatment) {
        return CatalogPrice.create(LISTA, ARTICULO, BillingCycle.MONTHLY, 1, null, 0,
                new BigDecimal("12000.00"), new BigDecimal("0.00"), rate, treatment, CREADO_EL);
    }

    @Nested
    @DisplayName("Creación")
    class Creacion {

        @Test
        @DisplayName("nace habilitado, sin id y con el ámbito que se le dio")
        void nace_con_su_ambito() {
            CatalogPrice precio = CatalogPriceMother.nuevoConTramo(1, null);

            assertThat(precio.getId()).isNull();
            assertThat(precio.isEnabled()).isTrue();
            assertThat(precio.getPriceListId()).isEqualTo(LISTA);
            assertThat(precio.getCatalogItemId()).isEqualTo(ARTICULO);
            assertThat(precio.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(precio.getCreatedDate()).isEqualTo(CREADO_EL);
        }

        @Test
        @DisplayName("normaliza los importes a dos decimales, que es la escala de la columna")
        void normaliza_los_importes() {
            CatalogPrice precio = conImportes(new BigDecimal("12000"), new BigDecimal("500.005"));

            assertThat(precio.getUnitAmount()).isEqualTo(new BigDecimal("12000.00"));
            assertThat(precio.getSetupAmount()).isEqualTo(new BigDecimal("500.01"));
        }

        @Test
        @DisplayName("normaliza también la tarifa: 19 y 19.00 son el mismo dato")
        void normaliza_la_tarifa() {
            CatalogPrice precio = conFiscalidad(new BigDecimal("19"), TaxTreatment.TAXED);

            assertThat(precio.getTaxRate()).isEqualTo(new BigDecimal("19.00"));
        }

        @Test
        @DisplayName("el ciclo anual lleva su propio importe, no un descuento sobre el mensual")
        void el_anual_lleva_su_propio_importe() {
            CatalogPrice anual = CatalogPrice.create(LISTA, ARTICULO, BillingCycle.ANNUAL, 1, null,
                    0, new BigDecimal("120000.00"), new BigDecimal("0.00"), new BigDecimal("19.00"),
                    TaxTreatment.TAXED, CREADO_EL);

            assertThat(anual.getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
            assertThat(anual.getUnitAmount()).isEqualTo(new BigDecimal("120000.00"));
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("exige la lista de precios")
        void exige_la_lista() {
            assertThatThrownBy(() -> CatalogPrice.create(null, ARTICULO, BillingCycle.MONTHLY, 1,
                    null, 0, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                    TaxTreatment.EXCLUDED, CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("priceListId is required");
        }

        @Test
        @DisplayName("exige el artículo del catálogo")
        void exige_el_articulo() {
            assertThatThrownBy(() -> CatalogPrice.create(LISTA, null, BillingCycle.MONTHLY, 1, null,
                    0, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, TaxTreatment.EXCLUDED,
                    CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("catalogItemId is required");
        }

        @Test
        @DisplayName("exige el ciclo de facturación")
        void exige_el_ciclo() {
            assertThatThrownBy(
                    () -> CatalogPrice.create(LISTA, ARTICULO, null, 1, null, 0, BigDecimal.ONE,
                            BigDecimal.ZERO, BigDecimal.ZERO, TaxTreatment.EXCLUDED, CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("billingCycle is required");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("el tramo mínimo arranca en 1, igual que chk_catalog_prices_tier")
        void rechaza_tramo_minimo_menor_que_uno(int tierMin) {
            assertThatThrownBy(() -> CatalogPriceMother.nuevoConTramo(tierMin, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tierMin must be 1 or greater");
        }

        @Test
        @DisplayName("el tramo máximo no puede quedar por debajo del mínimo")
        void rechaza_tramo_invertido() {
            assertThatThrownBy(() -> CatalogPriceMother.nuevoConTramo(10, 9))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tierMax must not be lower than tierMin");
        }

        @Test
        @DisplayName("un tramo de una sola unidad es válido")
        void tramo_de_una_unidad_es_valido() {
            assertThatCode(() -> CatalogPriceMother.nuevoConTramo(3, 3)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("la cantidad incluida no puede ser negativa")
        void rechaza_cantidad_incluida_negativa() {
            assertThatThrownBy(() -> CatalogPrice.create(LISTA, ARTICULO, BillingCycle.MONTHLY, 1,
                    null, -1, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                    TaxTreatment.EXCLUDED, CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("includedQuantity cannot be negative");
        }

        @Test
        @DisplayName("exige el precio unitario")
        void exige_el_precio_unitario() {
            assertThatThrownBy(() -> conImportes(null, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unitAmount is required");
        }

        @Test
        @DisplayName("rechaza un precio unitario negativo")
        void rechaza_precio_unitario_negativo() {
            assertThatThrownBy(() -> conImportes(new BigDecimal("-0.01"), BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unitAmount cannot be negative");
        }

        @Test
        @DisplayName("rechaza un cobro de puesta en marcha negativo")
        void rechaza_setup_negativo() {
            assertThatThrownBy(() -> conImportes(BigDecimal.ONE, new BigDecimal("-1.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("setupAmount cannot be negative");
        }
    }

    @Nested
    @DisplayName("Tratamiento fiscal")
    class Fiscalidad {

        @Test
        @DisplayName("EXEMPT y EXCLUDED son códigos distintos aunque los dos vayan a tarifa cero")
        void exento_y_excluido_no_se_colapsan() {
            CatalogPrice exento = conFiscalidad(BigDecimal.ZERO, TaxTreatment.EXEMPT);
            CatalogPrice excluido = conFiscalidad(BigDecimal.ZERO, TaxTreatment.EXCLUDED);

            assertThat(exento.getTaxRate()).isEqualByComparingTo(excluido.getTaxRate());
            assertThat(exento.getTaxTreatment()).isNotEqualTo(excluido.getTaxTreatment());
            assertThat(exento.getTaxTreatment()).isEqualTo(TaxTreatment.EXEMPT);
            assertThat(excluido.getTaxTreatment()).isEqualTo(TaxTreatment.EXCLUDED);
        }

        @Test
        @DisplayName("un precio gravado exige una tarifa por encima de cero")
        void gravado_exige_tarifa_positiva() {
            assertThatThrownBy(() -> conFiscalidad(BigDecimal.ZERO, TaxTreatment.TAXED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TAXED catalog price requires a tax rate above 0");
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"EXEMPT", "EXCLUDED"})
        @DisplayName("un precio no gravado con tarifa produce IVA sobre una base que no lo lleva")
        void no_gravado_con_tarifa_se_rechaza(TaxTreatment tratamiento) {
            assertThatThrownBy(() -> conFiscalidad(new BigDecimal("19.00"), tratamiento))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requires a tax rate of 0");
        }

        @Test
        @DisplayName("exige el tratamiento fiscal: sin él, tarifa cero es ambiguo")
        void exige_el_tratamiento() {
            assertThatThrownBy(() -> conFiscalidad(BigDecimal.ZERO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxTreatment is required");
        }

        @Test
        @DisplayName("exige la tarifa")
        void exige_la_tarifa() {
            assertThatThrownBy(() -> conFiscalidad(null, TaxTreatment.EXCLUDED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxRate is required");
        }

        @ParameterizedTest
        @ValueSource(strings = {"-0.01", "100.01"})
        @DisplayName("la tarifa vive entre 0 y 100")
        void rechaza_tarifa_fuera_de_rango(String rate) {
            assertThatThrownBy(() -> conFiscalidad(new BigDecimal(rate), TaxTreatment.TAXED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxRate must be between 0 and 100");
        }

        @Test
        @DisplayName("una tarifa del 100 % es válida")
        void tarifa_del_cien_es_valida() {
            assertThatCode(() -> conFiscalidad(new BigDecimal("100.00"), TaxTreatment.TAXED))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Solape de tramos")
    class Tramos {

        @ParameterizedTest
        @CsvSource({"1, 10, 5, 20", "5, 20, 1, 10", "1, 10, 10, 20", "1, 10, 1, 10",
                "3, 4, 1, 100"})
        @DisplayName("dos tramos que comparten al menos una unidad se pisan")
        void tramos_que_comparten_una_unidad_se_pisan(int minA, int maxA, int minB, int maxB) {
            CatalogPrice a = CatalogPriceMother.conTramo(1L, minA, maxA);

            assertThat(a.overlapsTier(minB, maxB)).isTrue();
        }

        @ParameterizedTest
        @CsvSource({"1, 10, 11, 20", "11, 20, 1, 10"})
        @DisplayName("dos tramos consecutivos sin unidad común no se pisan")
        void tramos_consecutivos_no_se_pisan(int minA, int maxA, int minB, int maxB) {
            CatalogPrice a = CatalogPriceMother.conTramo(1L, minA, maxA);

            assertThat(a.overlapsTier(minB, maxB)).isFalse();
        }

        @Test
        @DisplayName("un tramo abierto se pisa con cualquier cosa que empiece a su altura o después")
        void tramo_abierto_se_pisa_con_lo_que_venga_despues() {
            CatalogPrice abierto = CatalogPriceMother.conTramo(1L, 11, null);

            assertThat(abierto.overlapsTier(50, null)).isTrue();
            assertThat(abierto.overlapsTier(1, 10)).isFalse();
        }

        @Test
        @DisplayName("un tramo abierto contra Integer.MAX_VALUE no desborda la comparación")
        void tramo_abierto_no_desborda() {
            CatalogPrice abierto = CatalogPriceMother.conTramo(1L, 1, null);

            assertThat(abierto.overlapsTier(Integer.MAX_VALUE, Integer.MAX_VALUE)).isTrue();
        }

        @Test
        @DisplayName("requireNoTierOverlap deja pasar un candidato que no pisa a nadie")
        void deja_pasar_al_que_no_pisa() {
            CatalogPrice candidato = CatalogPriceMother.nuevoConTramo(11, 20);
            List<CatalogPrice> hermanos = List.of(CatalogPriceMother.conTramo(1L, 1, 10));

            assertThatCode(() -> CatalogPrice.requireNoTierOverlap(candidato, hermanos))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("requireNoTierOverlap rechaza al candidato que pisa a un hermano")
        void rechaza_al_que_pisa() {
            CatalogPrice candidato = CatalogPriceMother.nuevoConTramo(5, 20);
            List<CatalogPrice> hermanos = List.of(CatalogPriceMother.conTramo(99L, 1, 10));

            assertThatThrownBy(() -> CatalogPrice.requireNoTierOverlap(candidato, hermanos))
                    .isInstanceOfSatisfying(CatalogPriceTierOverlapException.class, ex -> {
                        assertThat(ex.getConflictingPriceId()).isEqualTo(99L);
                        assertThat(ex.getPriceListId()).isEqualTo(LISTA);
                        assertThat(ex.getCatalogItemId()).isEqualTo(ARTICULO);
                        assertThat(ex.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
                    });
        }

        @Test
        @DisplayName("editar un tramo sin moverlo no choca consigo mismo")
        void editar_sin_mover_no_choca_consigo_mismo() {
            CatalogPrice existente = CatalogPriceMother.conTramo(5L, 1, 10);
            List<CatalogPrice> hermanos = List.of(CatalogPriceMother.conTramo(5L, 1, 10));

            assertThatCode(() -> CatalogPrice.requireNoTierOverlap(existente, hermanos))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sin hermanos no hay nada que pisar")
        void sin_hermanos_no_hay_solape() {
            assertThatCode(() -> CatalogPrice
                    .requireNoTierOverlap(CatalogPriceMother.nuevoConTramo(1, null), List.of()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Edición")
    class Edicion {

        @Test
        @DisplayName("update cambia el tramo, los importes y la fiscalidad, no el ámbito")
        void update_no_reapunta_el_ambito() {
            CatalogPrice precio = CatalogPriceMother.conTramo(10L, 1, 10);

            precio.update(BillingCycle.ANNUAL, 11, 20, 5, new BigDecimal("9000"),
                    new BigDecimal("1000"), BigDecimal.ZERO, TaxTreatment.EXEMPT);

            assertThat(precio.getPriceListId()).isEqualTo(LISTA);
            assertThat(precio.getCatalogItemId()).isEqualTo(ARTICULO);
            assertThat(precio.getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
            assertThat(precio.getTierMin()).isEqualTo(11);
            assertThat(precio.getTierMax()).isEqualTo(20);
            assertThat(precio.getIncludedQuantity()).isEqualTo(5);
            assertThat(precio.getUnitAmount()).isEqualTo(new BigDecimal("9000.00"));
            assertThat(precio.getSetupAmount()).isEqualTo(new BigDecimal("1000.00"));
            assertThat(precio.getTaxTreatment()).isEqualTo(TaxTreatment.EXEMPT);
        }

        @Test
        @DisplayName("update aplica las mismas invariantes fiscales que la creación")
        void update_valida_la_fiscalidad() {
            CatalogPrice precio = CatalogPriceMother.mensualGravado();

            assertThatThrownBy(
                    () -> precio.update(BillingCycle.MONTHLY, 1, null, 0, new BigDecimal("12000"),
                            BigDecimal.ZERO, new BigDecimal("19.00"), TaxTreatment.EXCLUDED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requires a tax rate of 0");
        }

        @Test
        @DisplayName("la baja lógica no toca importes")
        void la_baja_no_toca_importes() {
            CatalogPrice precio = CatalogPriceMother.mensualGravado();

            precio.disable();

            assertThat(precio.isEnabled()).isFalse();
            assertThat(precio.getUnitAmount()).isEqualTo(new BigDecimal("12000.00"));
        }
    }
}
