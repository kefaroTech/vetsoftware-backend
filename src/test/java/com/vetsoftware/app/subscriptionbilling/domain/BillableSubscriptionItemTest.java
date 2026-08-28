package com.vetsoftware.app.subscriptionbilling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * <b>La puerta que impide facturarle la tarifa completa a quien esta en periodo
 * de prueba.</b>
 *
 * <p>
 * <b>Por que esta prueba vive aqui y no en la rodaja de persistencia.</b>
 * {@code JpaBillableSubscriptionItemPort} <em>proyecta</em>
 * {@code charge_mode}, no lo filtra: su {@code WHERE} solo mira empresa,
 * contrato y vigencia. La unica linea de codigo que decide si algo se cobra es
 * {@link BillableSubscriptionItem#devenga(LocalDate)}, y por eso es la que hay
 * que sujetar.
 *
 * <p>
 * <b>La regla no se puede probar en vacio, y ese es el riesgo.</b> Hoy
 * <em>ninguna</em> linea nace en prueba: la columna tiene defecto
 * {@code 'PAID'} y {@code SubscriptionItemJpaEntity} lo repite en Java. Una
 * prueba escrita sobre el estado actual del catalogo pasaria sin ejercitar
 * nunca la rama —y seguiria pasando el dia que se rompa—. Todos los casos de
 * abajo construyen a proposito la linea que hoy no existe: en prueba, gratuita
 * con techo o vencida, y <b>con su tarifa real dentro</b>.
 *
 * <p>
 * <b>Por que la tarifa importa</b> (R-TRIAL-14). Una linea {@code TRIAL} no
 * lleva {@code unit_amount = 0}: conserva el precio que se cobrara cuando la
 * prueba termine. La consecuencia es que olvidar el modo no produce ceros
 * sospechosos, produce <b>la cuota entera</b>, bien formada, a todos los
 * clientes en prueba el mismo mes. Por eso las lineas de estos casos valen
 * 179.000 y no cero: si la regla se rompiera, la asercion de importe es la que
 * ensena cuanto se cobra de mas.
 */
@DisplayName("BillableSubscriptionItem — que devenga y que no")
class BillableSubscriptionItemTest {

    private static final LocalDate ARRANQUE = LocalDate.of(2026, 3, 1);

    /** La tarifa real que la linea guarda aunque no se este cobrando. */
    private static final BigDecimal TARIFA = new BigDecimal("179000.00");

    private static BillableSubscriptionItem linea(ItemChargeMode modo, LocalDate desde,
            LocalDate hasta) {
        return new BillableSubscriptionItem(900L, 9L, 90L, 5L, "Historia clinica", modo, 1, 0,
                TARIFA, new BigDecimal("19.00"), TaxTreatment.TAXED, desde, hasta);
    }

    private static BillableSubscriptionItem lineaVigente(ItemChargeMode modo) {
        return linea(modo, ARRANQUE.minusMonths(1), null);
    }

    @Nested
    @DisplayName("El modo de cobro es el filtro")
    class ModoDeCobro {

        @ParameterizedTest
        @EnumSource(value = ItemChargeMode.class, names = "PAID", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("ninguna linea que no sea PAID devenga, aunque este vigente y con tarifa")
        void ninguna_linea_que_no_sea_paid_devenga(ItemChargeMode modo) {
            // @EnumSource excluyendo PAID y no tres @Test sueltos: asi un quinto modo
            // de cobro añadido al enum entra solo en esta matriz. Un modo nuevo que se
            // colara como devengable es exactamente el fallo que nadie ve, porque la
            // factura sale bien formada.
            assertThat(lineaVigente(modo).devenga(ARRANQUE)).isFalse();
        }

        @Test
        @DisplayName("PAID es el unico modo que devenga")
        void paid_es_el_unico_que_devenga() {
            assertThat(lineaVigente(ItemChargeMode.PAID).devenga(ARRANQUE)).isTrue();
        }

        @Test
        @DisplayName("la linea en prueba conserva su tarifa completa: es lo que se cobraria de"
                + " mas si el modo se olvidara")
        void la_linea_en_prueba_conserva_su_tarifa_completa() {
            BillableSubscriptionItem enPrueba = lineaVigente(ItemChargeMode.TRIAL);

            assertThat(enPrueba.devenga(ARRANQUE)).isFalse();
            // El cero no esta en el importe: esta en la decision. Si la puerta se
            // rompiera, esto es lo que se le facturaria a un cliente que esta probando.
            assertThat(enPrueba.recurringSubtotal()).isEqualByComparingTo(TARIFA);
        }

        @Test
        @DisplayName("la linea gratuita con techo tampoco devenga y tambien lleva tarifa")
        void la_linea_gratuita_con_techo_tampoco_devenga() {
            BillableSubscriptionItem gratuita = lineaVigente(ItemChargeMode.FREE_LIMITED);

            assertThat(gratuita.devenga(ARRANQUE)).isFalse();
            assertThat(gratuita.recurringSubtotal()).isEqualByComparingTo(TARIFA);
        }
    }

    @Nested
    @DisplayName("Vigencia semiabierta [effectiveFrom, effectiveTo)")
    class Vigencia {

        @Test
        @DisplayName("el dia en que arranca la linea ya cuenta")
        void el_dia_de_arranque_ya_cuenta() {
            assertThat(linea(ItemChargeMode.PAID, ARRANQUE, null).vigenteEn(ARRANQUE)).isTrue();
        }

        @Test
        @DisplayName("la vispera del arranque no cuenta")
        void la_vispera_no_cuenta() {
            assertThat(linea(ItemChargeMode.PAID, ARRANQUE.plusDays(1), null).vigenteEn(ARRANQUE))
                    .isFalse();
        }

        @Test
        @DisplayName("el dia del cierre pertenece a la sucesora, no a esta linea")
        void el_dia_del_cierre_pertenece_a_la_sucesora() {
            // Con el extremo cerrado se cobrarian las dos lineas ese dia: la que se
            // cierra y la que la sucede.
            assertThat(linea(ItemChargeMode.PAID, ARRANQUE.minusMonths(1), ARRANQUE)
                    .vigenteEn(ARRANQUE)).isFalse();
        }

        @Test
        @DisplayName("la vispera del cierre todavia es de esta linea")
        void la_vispera_del_cierre_todavia_es_de_esta_linea() {
            assertThat(linea(ItemChargeMode.PAID, ARRANQUE.minusMonths(1), ARRANQUE.plusDays(1))
                    .vigenteEn(ARRANQUE)).isTrue();
        }

        @Test
        @DisplayName("sin fecha de cierre la linea sigue viva")
        void sin_fecha_de_cierre_sigue_viva() {
            assertThat(linea(ItemChargeMode.PAID, ARRANQUE.minusMonths(1), null)
                    .vigenteEn(ARRANQUE.plusYears(5))).isTrue();
        }

        @Test
        @DisplayName("una linea PAID ya cerrada no devenga: las dos condiciones son necesarias")
        void una_linea_paid_ya_cerrada_no_devenga() {
            assertThat(
                    linea(ItemChargeMode.PAID, ARRANQUE.minusMonths(1), ARRANQUE).devenga(ARRANQUE))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Lo que de verdad se cobra")
    class Cantidad {

        @Test
        @DisplayName("lo incluido se descuenta de la cantidad")
        void lo_incluido_se_descuenta() {
            BillableSubscriptionItem cinco = new BillableSubscriptionItem(900L, 9L, 90L, 5L, "Sede",
                    ItemChargeMode.PAID, 5, 3, new BigDecimal("10000.00"), new BigDecimal("19.00"),
                    TaxTreatment.TAXED, ARRANQUE, null);

            assertThat(cinco.billableQuantity()).isEqualTo(2);
            assertThat(cinco.recurringSubtotal()).isEqualByComparingTo("20000.00");
        }

        @Test
        @DisplayName("consumir menos de lo incluido no genera un abono: se corta en cero")
        void consumir_menos_de_lo_incluido_no_genera_abono() {
            // Sin el max(...,0) esto seria un subtotal NEGATIVO, es decir, un abono
            // dentro de una factura de cuota por no haber gastado todo lo contratado.
            BillableSubscriptionItem dos = new BillableSubscriptionItem(900L, 9L, 90L, 5L, "Sede",
                    ItemChargeMode.PAID, 2, 3, new BigDecimal("10000.00"), new BigDecimal("19.00"),
                    TaxTreatment.TAXED, ARRANQUE, null);

            assertThat(dos.billableQuantity()).isZero();
            assertThat(dos.recurringSubtotal()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Invariantes de construccion")
    class Invariantes {

        @Test
        @DisplayName("exige el modo de cobro: sin el no se puede decidir si devenga")
        void exige_el_modo_de_cobro() {
            assertThatThrownBy(() -> new BillableSubscriptionItem(900L, 9L, 90L, 5L, "Sede", null,
                    1, 0, TARIFA, new BigDecimal("19.00"), TaxTreatment.TAXED, ARRANQUE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chargeMode is required");
        }

        @Test
        @DisplayName("exige la empresa: es lo que acota la linea a un tenant")
        void exige_la_empresa() {
            assertThatThrownBy(() -> new BillableSubscriptionItem(900L, null, 90L, 5L, "Sede",
                    ItemChargeMode.PAID, 1, 0, TARIFA, new BigDecimal("19.00"), TaxTreatment.TAXED,
                    ARRANQUE, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("rechaza una tarifa negativa")
        void rechaza_una_tarifa_negativa() {
            assertThatThrownBy(() -> new BillableSubscriptionItem(900L, 9L, 90L, 5L, "Sede",
                    ItemChargeMode.PAID, 1, 0, new BigDecimal("-1.00"), new BigDecimal("19.00"),
                    TaxTreatment.TAXED, ARRANQUE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unitAmount cannot be negative");
        }

        @Test
        @DisplayName("rechaza una cantidad negativa")
        void rechaza_una_cantidad_negativa() {
            assertThatThrownBy(() -> new BillableSubscriptionItem(900L, 9L, 90L, 5L, "Sede",
                    ItemChargeMode.PAID, -1, 0, TARIFA, new BigDecimal("19.00"), TaxTreatment.TAXED,
                    ARRANQUE, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity cannot be negative");
        }
    }
}
