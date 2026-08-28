package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("SubscriptionItem - lo contratado, con fechas")
class SubscriptionItemTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final Long ARTICULO = 100L;
    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JUNIO_30 = LocalDate.of(2026, 6, 30);
    private static final BigDecimal PRECIO = new BigDecimal("179000.00");

    private static SubscriptionItem lineaAbierta() {
        return SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "EXTRA_USER", "Usuario adicional",
                SubscriptionItemType.CAPACITY, "USER", 2, TaxTreatment.TAXED, 5, PRECIO,
                new BigDecimal("19.00"), EffectivePeriod.openFrom(ENERO_1), ItemOrigin.ADDON, 11L);
    }

    @Nested
    @DisplayName("Dar de baja no borra")
    class DarDeBajaNoBorra {

        @Test
        @DisplayName("cerrar una linea solo le escribe la fecha de fin")
        void cerrarSoloEscribeLaFecha() {
            SubscriptionItem linea = lineaAbierta();

            linea.endOn(JUNIO_30, 99L);

            assertThat(linea.getPeriod().to()).isEqualTo(JUNIO_30);
            assertThat(linea.getPeriod().from()).isEqualTo(ENERO_1);
            assertThat(linea.getEndedAmendmentId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("cerrar NO desactiva la fila: enabled sigue en true")
        void cerrarNoDesactiva() {
            SubscriptionItem linea = lineaAbierta();

            linea.endOn(JUNIO_30, 99L);

            // R12: dar de baja un modulo jamas destruye ni oculta informacion del
            // cliente. Poner enabled = false la haria invisible a @SQLRestriction y con
            // ella desapareceria la prueba de que ese modulo estuvo contratado.
            assertThat(linea.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la linea cerrada sigue estando vigente en las fechas que cubrio")
        void siguePudiendoConsultarseElPasado() {
            SubscriptionItem linea = lineaAbierta();
            linea.endOn(JUNIO_30, 99L);

            assertThat(linea.isCurrentOn(LocalDate.of(2026, 3, 15))).isTrue();
            assertThat(linea.isCurrentOn(LocalDate.of(2026, 8, 1))).isFalse();
        }

        @Test
        @DisplayName("cerrar dos veces reescribiria una fecha que ya es historia")
        void cerrarDosVeces() {
            SubscriptionItem linea = lineaAbierta();
            linea.endOn(JUNIO_30, 99L);

            assertThatThrownBy(() -> linea.endOn(LocalDate.of(2026, 9, 1), 100L))
                    .isInstanceOf(SubscriptionItemAlreadyEndedException.class);
        }

        @Test
        @DisplayName("una linea con otrosi de cierre pero sin fecha de fin no puede existir")
        void cerradaSinFecha() {
            assertThatThrownBy(() -> new SubscriptionItem(1L, EMPRESA, CONTRATO, ARTICULO, "CORE",
                    "Nucleo", SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1, PRECIO,
                    BigDecimal.ZERO, EffectivePeriod.openFrom(ENERO_1), ItemOrigin.INITIAL, null,
                    99L, null, 0L, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("endedAmendmentId");
        }
    }

    @Nested
    @DisplayName("Lo congelado se queda congelado")
    class Congelacion {

        @Test
        @DisplayName("la sucesora conserva el precio unitario de la original")
        void laSucesoraConservaElPrecio() {
            SubscriptionItem original = lineaAbierta();

            SubscriptionItem sucesora = original.withQuantity(8, JUNIO_30, 99L);

            // Cambiar de precio es cerrar y abrir con otro precio; cambiar de cantidad
            // no toca el precio. Si la sucesora releyera la tarifa, un cliente que
            // amplia usuarios acabaria pagando la tarifa nueva por todo el paquete.
            assertThat(sucesora.getUnitAmount()).isEqualByComparingTo(PRECIO);
        }

        @Test
        @DisplayName("la sucesora conserva included_quantity: es la congelacion al firmar")
        void laSucesoraConservaLoIncluido() {
            SubscriptionItem original = lineaAbierta();

            SubscriptionItem sucesora = original.withQuantity(8, JUNIO_30, 99L);

            // La causa numero uno de sobrefacturacion en modelos de suscripcion: si lo
            // incluido se releyera de la tarifa, editar un tramo cambiaria
            // retroactivamente cuantos usuarios le sobran a quien firmo hace un ano.
            assertThat(sucesora.getIncludedQuantity()).isEqualTo(2);
            assertThat(sucesora.getTaxRate()).isEqualByComparingTo(original.getTaxRate());
            assertThat(sucesora.getTaxTreatment()).isEqualTo(original.getTaxTreatment());
        }

        @Test
        @DisplayName("la sucesora nace abierta, con la cantidad nueva y origen QUANTITY_CHANGE")
        void laSucesoraNaceAbierta() {
            SubscriptionItem sucesora = lineaAbierta().withQuantity(8, JUNIO_30, 99L);

            assertThat(sucesora.getQuantity()).isEqualTo(8);
            assertThat(sucesora.getOrigin()).isEqualTo(ItemOrigin.QUANTITY_CHANGE);
            assertThat(sucesora.getPeriod().from()).isEqualTo(JUNIO_30);
            assertThat(sucesora.getPeriod().isOpen()).isTrue();
            assertThat(sucesora.getId()).isNull();
        }

        @Test
        @DisplayName("original y sucesora no se pisan: la original cierra donde la otra abre")
        void originalYSucesoraNoSePisan() {
            SubscriptionItem original = lineaAbierta();
            SubscriptionItem sucesora = original.withQuantity(8, JUNIO_30, 99L);
            original.endOn(JUNIO_30, 99L);

            assertThat(original.overlaps(sucesora.getPeriod())).isFalse();
        }

        @Test
        @DisplayName("lo facturable descuenta lo incluido y nunca baja de cero")
        void loFacturableDescuentaLoIncluido() {
            SubscriptionItem cincoUsuariosConDosIncluidos = lineaAbierta();
            assertThat(cincoUsuariosConDosIncluidos.billableQuantity()).isEqualTo(3);

            SubscriptionItem anaTrabajaSola = SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO,
                    "EXTRA_USER", "Usuario adicional", SubscriptionItemType.CAPACITY, "USER", 2,
                    TaxTreatment.TAXED, 1, PRECIO, BigDecimal.ZERO,
                    EffectivePeriod.openFrom(ENERO_1), ItemOrigin.INITIAL, null);

            // Ana trabaja sola, el nucleo incluye 2 usuarios: se le cobran cero, no uno.
            assertThat(anaTrabajaSola.billableQuantity()).isZero();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una capacidad sin unidad no se puede contar")
        void capacidadSinUnidad() {
            assertThatThrownBy(() -> SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "EXTRA",
                    "Extra", SubscriptionItemType.CAPACITY, null, 0, TaxTreatment.TAXED, 1, PRECIO,
                    BigDecimal.ZERO, EffectivePeriod.openFrom(ENERO_1), ItemOrigin.ADDON, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("capacityUnit");
        }

        @Test
        @DisplayName("una unidad colgada de un modulo no significa nada")
        void moduloConUnidad() {
            assertThatThrownBy(() -> SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "CORE",
                    "Nucleo", SubscriptionItemType.MODULE, "USER", 0, TaxTreatment.TAXED, 1, PRECIO,
                    BigDecimal.ZERO, EffectivePeriod.openFrom(ENERO_1), ItemOrigin.INITIAL, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("capacityUnit");
        }

        @Test
        @DisplayName("la cantidad tiene que ser positiva")
        void cantidadCero() {
            assertThatThrownBy(() -> SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "CORE",
                    "Nucleo", SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 0, PRECIO,
                    BigDecimal.ZERO, EffectivePeriod.openFrom(ENERO_1), ItemOrigin.INITIAL, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("quantity");
        }

        @Test
        @DisplayName("el importe no admite mas de dos decimales: la columna es DECIMAL(19,2)")
        void importeConTresDecimales() {
            assertThatThrownBy(() -> SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "CORE",
                    "Nucleo", SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                    new BigDecimal("1000.123"), BigDecimal.ZERO, EffectivePeriod.openFrom(ENERO_1),
                    ItemOrigin.INITIAL, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("decimals");
        }

        @Test
        @DisplayName("la tarifa de IVA vive entre 0 y 100")
        void tarifaFueraDeRango() {
            assertThatThrownBy(() -> SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "CORE",
                    "Nucleo", SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1, PRECIO,
                    new BigDecimal("101.00"), EffectivePeriod.openFrom(ENERO_1), ItemOrigin.INITIAL,
                    null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxRate");
        }
    }

    /**
     * Cada guarda del constructor es un CHECK o un NOT NULL de
     * {@code subscription_items} escrito en Java. Si se relaja una, la fila se
     * escribe igual y el que revienta es MySQL, en mitad de un alta a medio hacer.
     */
    private static SubscriptionItem linea(Long companyId, Long catalogItemId, String itemCode,
            String itemName, SubscriptionItemType itemType, String capacityUnit,
            int includedQuantity, TaxTreatment taxTreatment, int quantity, BigDecimal unitAmount,
            BigDecimal taxRate, EffectivePeriod period, ItemOrigin origin) {
        return SubscriptionItem.open(companyId, CONTRATO, catalogItemId, itemCode, itemName,
                itemType, capacityUnit, includedQuantity, taxTreatment, quantity, unitAmount,
                taxRate, period, origin, null);
    }

    @Nested
    @DisplayName("Invariantes de la fila")
    class Invariantes {

        static Stream<Arguments> filasInvalidas() {
            EffectivePeriod abierta = EffectivePeriod.openFrom(ENERO_1);
            String cincuentaYUno = "C".repeat(51);
            String cientoVeintiuno = "N".repeat(121);
            return Stream.of(
                    Arguments.of("sin empresa",
                            (ThrowingCallable) () -> linea(null, ARTICULO, "CORE", "Nucleo",
                                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                                    PRECIO, BigDecimal.ZERO, abierta, ItemOrigin.INITIAL),
                            "companyId"),
                    Arguments.of("sin articulo de catalogo",
                            (ThrowingCallable) () -> linea(EMPRESA, null, "CORE", "Nucleo",
                                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                                    PRECIO, BigDecimal.ZERO, abierta, ItemOrigin.INITIAL),
                            "catalogItemId"),
                    Arguments.of("codigo en blanco",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "  ", "Nucleo",
                                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                                    PRECIO, BigDecimal.ZERO, abierta, ItemOrigin.INITIAL),
                            "itemCode is required"),
                    Arguments.of("codigo mas largo que la columna",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, cincuentaYUno,
                                    "Nucleo", SubscriptionItemType.MODULE, null, 0,
                                    TaxTreatment.TAXED, 1, PRECIO, BigDecimal.ZERO, abierta,
                                    ItemOrigin.INITIAL),
                            "itemCode must be 50"),
                    Arguments.of("nombre en blanco",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE", " ",
                                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                                    PRECIO, BigDecimal.ZERO, abierta, ItemOrigin.INITIAL),
                            "itemName is required"),
                    Arguments.of("nombre mas largo que la columna",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE",
                                    cientoVeintiuno, SubscriptionItemType.MODULE, null, 0,
                                    TaxTreatment.TAXED, 1, PRECIO, BigDecimal.ZERO, abierta,
                                    ItemOrigin.INITIAL),
                            "itemName must be 120"),
                    Arguments.of("sin tipo de articulo",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE", "Nucleo",
                                    null, null, 0, TaxTreatment.TAXED, 1, PRECIO, BigDecimal.ZERO,
                                    abierta, ItemOrigin.INITIAL),
                            "itemType"),
                    Arguments.of("sin tratamiento fiscal",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE", "Nucleo",
                                    SubscriptionItemType.MODULE, null, 0, null, 1, PRECIO,
                                    BigDecimal.ZERO, abierta, ItemOrigin.INITIAL),
                            "taxTreatment"),
                    Arguments.of("lo incluido no puede ser negativo",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE", "Nucleo",
                                    SubscriptionItemType.MODULE, null, -1, TaxTreatment.TAXED, 1,
                                    PRECIO, BigDecimal.ZERO, abierta, ItemOrigin.INITIAL),
                            "includedQuantity"),
                    Arguments.of("sin precio unitario",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE", "Nucleo",
                                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                                    null, BigDecimal.ZERO, abierta, ItemOrigin.INITIAL),
                            "unitAmount"),
                    Arguments.of("precio unitario negativo",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE", "Nucleo",
                                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                                    new BigDecimal("-1.00"), BigDecimal.ZERO, abierta,
                                    ItemOrigin.INITIAL),
                            "unitAmount"),
                    Arguments.of("sin tarifa de IVA",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE", "Nucleo",
                                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                                    PRECIO, null, abierta, ItemOrigin.INITIAL),
                            "taxRate"),
                    Arguments.of("sin vigencia",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE", "Nucleo",
                                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                                    PRECIO, BigDecimal.ZERO, null, ItemOrigin.INITIAL),
                            "effective period"),
                    Arguments.of("sin origen",
                            (ThrowingCallable) () -> linea(EMPRESA, ARTICULO, "CORE", "Nucleo",
                                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                                    PRECIO, BigDecimal.ZERO, abierta, null),
                            "origin"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("filasInvalidas")
        @DisplayName("la fila rechaza lo mismo que rechazaria la base")
        void filasInvalidas(String caso, ThrowingCallable creacion, String fragmento) {
            assertThatThrownBy(creacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(fragmento);
        }

        @Test
        @DisplayName("cerrar sin fecha de fin falla: dar de baja es escribir una fecha")
        void cerrarSinFecha() {
            SubscriptionItem linea = lineaAbierta();

            assertThatThrownBy(() -> linea.endOn(null, 99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("effectiveTo");
        }
    }

    @Nested
    @DisplayName("Una fila deshabilitada no cuenta")
    class Deshabilitada {

        private static SubscriptionItem deshabilitada() {
            return new SubscriptionItem(5L, EMPRESA, CONTRATO, ARTICULO, "CORE", "Nucleo",
                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1, PRECIO,
                    BigDecimal.ZERO, EffectivePeriod.openFrom(ENERO_1), ItemOrigin.INITIAL, null,
                    null, null, 3L, false);
        }

        @Test
        @DisplayName("no esta vigente ningun dia, aunque su tramo lo cubra")
        void noEstaVigente() {
            // enabled entra en el criterio porque @SQLRestriction tambien lo cuenta: si
            // el dominio dijera que si y la consulta no la devolviera, el mismo contrato
            // tendria dos respuestas distintas segun quien preguntara.
            assertThat(deshabilitada().isCurrentOn(LocalDate.of(2026, 3, 15))).isFalse();
        }

        @Test
        @DisplayName("no bloquea un alta: no se pisa con nada")
        void noBloqueaUnAlta() {
            assertThat(deshabilitada().overlaps(EffectivePeriod.openFrom(ENERO_1))).isFalse();
        }

        @Test
        @DisplayName("conserva id y version para el bloqueo optimista")
        void conservaIdYVersion() {
            assertThat(deshabilitada().getId()).isEqualTo(5L);
            assertThat(deshabilitada().getVersion()).isEqualTo(3L);
        }
    }
}
