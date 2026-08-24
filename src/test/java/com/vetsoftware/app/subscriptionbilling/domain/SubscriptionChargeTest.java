package com.vetsoftware.app.subscriptionbilling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SubscriptionCharge — lo devengado, que no se edita ni se borra")
class SubscriptionChargeTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-22T10:15:30Z"),
            ZoneId.of("America/Bogota"));
    private static final ServicePeriod AGOSTO = new ServicePeriod(LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31));

    private static SubscriptionCharge cuota(BigDecimal subtotal) {
        return SubscriptionCharge.create(42L, 7L, 3L, ChargeType.RECURRING, "Plan CORE agosto",
                AGOSTO, BigDecimal.ONE, subtotal, subtotal, new BigDecimal("19.00"),
                TaxTreatment.TAXED, null, null, RELOJ);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("nace PENDING y sin documento: devengar y facturar son dos cosas distintas")
        void nace_pendiente_y_sin_documento() {
            SubscriptionCharge charge = cuota(new BigDecimal("179000.00"));

            assertThat(charge.getStatus()).isEqualTo(ChargeStatus.PENDING);
            assertThat(charge.getBillingDocumentId()).isNull();
            assertThat(charge.esFacturable()).isTrue();
            assertThat(charge.getCreatedDate()).isEqualTo("2026-08-22T05:15:30");
        }

        @Test
        @DisplayName("guarda los dos numeros del prorrateo, que es lo que lo hace reconstruible")
        void guarda_la_base_del_prorrateo() {
            SubscriptionCharge charge = SubscriptionCharge.create(42L, 7L, 3L, ChargeType.PRORATION,
                    "Ampliacion a mitad de ciclo", AGOSTO, BigDecimal.ONE,
                    new BigDecimal("90000.00"), new BigDecimal("34838.71"), new BigDecimal("19.00"),
                    TaxTreatment.TAXED, new ProrationBasis(12, 31), null, RELOJ);

            assertThat(charge.getProration()).isEqualTo(new ProrationBasis(12, 31));
        }

        @Test
        @DisplayName("no existe ningun importe de impuesto: el cargo guarda su base, no su impuesto")
        void no_declara_importe_de_impuesto() {
            List<String> campos = Arrays.stream(SubscriptionCharge.class.getDeclaredFields())
                    .map(Field::getName).toList();

            assertThat(campos).contains("subtotalAmount", "taxRate", "taxTreatment")
                    .doesNotContain("taxAmount");
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("la cantidad tiene que ser mayor que cero")
        void cantidad_positiva() {
            assertThatThrownBy(() -> SubscriptionCharge.create(42L, 7L, null, ChargeType.RECURRING,
                    "x", AGOSTO, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN,
                    new BigDecimal("19.00"), TaxTreatment.TAXED, null, null, RELOJ))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("quantity");
        }

        @Test
        @DisplayName("el periodo de servicio no puede terminar antes de empezar")
        void periodo_coherente() {
            assertThatThrownBy(
                    () -> new ServicePeriod(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("before start");
        }

        @Test
        @DisplayName("un prorrateo con un solo numero no cabe: o estan los dos o no esta ninguno")
        void prorrateo_completo_o_ausente() {
            assertThatThrownBy(() -> ProrationBasis.of(12, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("go together");
            assertThatThrownBy(() -> ProrationBasis.of(null, 31))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(ProrationBasis.of(null, null)).isNull();
        }

        @Test
        @DisplayName("los dias cobrados no pueden superar los del periodo")
        void prorrateo_acotado() {
            assertThatThrownBy(() -> new ProrationBasis(40, 31))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot exceed");
        }

        @Test
        @DisplayName("TAXED con tarifa cero no cabe: seria un desglose fiscal inconstruible")
        void gravado_exige_tarifa() {
            assertThatThrownBy(() -> cuotaCon(TaxTreatment.TAXED, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("excluido y exento exigen tarifa cero, y siguen siendo tratamientos distintos")
        void excluido_y_exento_no_son_lo_mismo() {
            assertThatThrownBy(() -> cuotaCon(TaxTreatment.EXEMPT, new BigDecimal("19.00")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> cuotaCon(TaxTreatment.EXCLUDED, new BigDecimal("19.00")))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(cuotaCon(TaxTreatment.EXEMPT, BigDecimal.ZERO).getTaxTreatment())
                    .isNotEqualTo(
                            cuotaCon(TaxTreatment.EXCLUDED, BigDecimal.ZERO).getTaxTreatment());
        }

        @Test
        @DisplayName("un cargo INVOICED sin documento detras no se puede construir")
        void facturado_exige_documento() {
            assertThatThrownBy(() -> new SubscriptionCharge(1L, 42L, 7L, null, ChargeType.RECURRING,
                    "x", AGOSTO, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN,
                    new BigDecimal("19.00"), TaxTreatment.TAXED, null, ChargeStatus.INVOICED, null,
                    null, null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("billing document");
        }

        @Test
        @DisplayName("solo un CREDIT puede apuntar al cargo que compensa")
        void solo_el_credito_compensa() {
            assertThatThrownBy(() -> new SubscriptionCharge(1L, 42L, 7L, null, ChargeType.RECURRING,
                    "x", AGOSTO, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN,
                    new BigDecimal("19.00"), TaxTreatment.TAXED, null, ChargeStatus.PENDING, null,
                    null, 99L, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only a CREDIT charge");
        }

        private SubscriptionCharge cuotaCon(TaxTreatment treatment, BigDecimal rate) {
            return SubscriptionCharge.create(42L, 7L, null, ChargeType.RECURRING, "x", AGOSTO,
                    BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, rate, treatment, null, null,
                    RELOJ);
        }
    }

    @Nested
    @DisplayName("Convencion de signos — TRAMPA 1")
    class ConvencionDeSignos {

        @Test
        @DisplayName("RECURRING y ONE_TIME no admiten subtotal negativo")
        void los_que_suman_no_restan() {
            assertThatThrownBy(() -> conTipoYSubtotal(ChargeType.RECURRING, "-1.00"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative subtotal");
            assertThatThrownBy(() -> conTipoYSubtotal(ChargeType.ONE_TIME, "-1.00"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("CREDIT y DISCOUNT no admiten subtotal positivo")
        void los_que_restan_no_suman() {
            assertThatThrownBy(() -> conTipoYSubtotal(ChargeType.CREDIT, "1.00"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive subtotal");
            assertThatThrownBy(() -> conTipoYSubtotal(ChargeType.DISCOUNT, "1.00"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("PRORATION admite los dos signos: ampliar cobra y reducir acredita")
        void el_prorrateo_es_libre_de_signo() {
            assertThat(conTipoYSubtotal(ChargeType.PRORATION, "34838.71").signo()).isOne();
            assertThat(conTipoYSubtotal(ChargeType.PRORATION, "-34838.71").signo()).isNegative();
        }

        @Test
        @DisplayName("la anulacion es un cargo negativo que sumado al original da cero,"
                + " y los dos quedan")
        void anular_es_compensar_y_los_dos_quedan() {
            SubscriptionCharge original = new SubscriptionCharge(500L, 42L, 7L, 3L,
                    ChargeType.RECURRING, "Plan CORE agosto", AGOSTO, BigDecimal.ONE,
                    new BigDecimal("179000.00"), new BigDecimal("179000.00"),
                    new BigDecimal("19.00"), TaxTreatment.TAXED, null, ChargeStatus.PENDING, null,
                    null, null, null);

            SubscriptionCharge compensacion = SubscriptionCharge.voidingOf(original,
                    "Anulacion de la cuota de agosto", RELOJ);

            assertThat(compensacion.getSubtotalAmount()).isEqualByComparingTo("-179000.00");
            assertThat(compensacion.getSubtotalAmount().add(original.getSubtotalAmount()))
                    .isEqualByComparingTo("0.00");
            assertThat(compensacion.getChargeType()).isEqualTo(ChargeType.CREDIT);
            assertThat(compensacion.getVoidsChargeId()).isEqualTo(500L);
            assertThat(original.getStatus()).isEqualTo(ChargeStatus.VOIDED);
        }

        @Test
        @DisplayName("la compensacion hereda el periodo y la tarifa: el desglose del credito"
                + " se agrupa igual que el del cargo original")
        void la_compensacion_hereda_periodo_y_tarifa() {
            SubscriptionCharge original = persistido("179000.00");

            SubscriptionCharge compensacion = SubscriptionCharge.voidingOf(original, "x", RELOJ);

            assertThat(compensacion.getServicePeriod()).isEqualTo(original.getServicePeriod());
            assertThat(compensacion.getTaxRate()).isEqualByComparingTo(original.getTaxRate());
            assertThat(compensacion.getTaxTreatment()).isEqualTo(original.getTaxTreatment());
        }

        @Test
        @DisplayName("un cargo ya anulado no se vuelve a anular")
        void no_se_anula_dos_veces() {
            SubscriptionCharge original = persistido("179000.00");
            SubscriptionCharge.voidingOf(original, "primera", RELOJ);

            assertThatThrownBy(() -> SubscriptionCharge.voidingOf(original, "segunda", RELOJ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already voided");
        }

        @Test
        @DisplayName("compensar un cargo ya negativo no cabe en el esquema, y se dice por que")
        void el_esquema_solo_expresa_la_compensacion_de_un_positivo() {
            SubscriptionCharge credito = new SubscriptionCharge(600L, 42L, 7L, null,
                    ChargeType.CREDIT, "Devolucion", AGOSTO, BigDecimal.ONE,
                    new BigDecimal("50000.00"), new BigDecimal("-50000.00"), BigDecimal.ZERO,
                    TaxTreatment.EXCLUDED, null, ChargeStatus.PENDING, null, null, null, null);

            assertThatThrownBy(() -> SubscriptionCharge.voidingOf(credito, "x", RELOJ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chk_subscription_charges_voids");
        }

        private SubscriptionCharge conTipoYSubtotal(ChargeType type, String subtotal) {
            return SubscriptionCharge.create(42L, 7L, null, type, "x", AGOSTO, BigDecimal.ONE,
                    new BigDecimal("179000.00"), new BigDecimal(subtotal), BigDecimal.ZERO,
                    TaxTreatment.EXCLUDED, null, null, RELOJ);
        }
    }

    @Nested
    @DisplayName("Inmutabilidad")
    class Inmutabilidad {

        @Test
        @DisplayName("solo status y billingDocumentId son mutables; el resto son final")
        void todo_final_salvo_los_dos_campos_de_sellado() {
            List<String> mutables = Arrays.stream(SubscriptionCharge.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic()).filter(f -> !Modifier.isStatic(f.getModifiers()))
                    .filter(f -> !Modifier.isFinal(f.getModifiers())).map(Field::getName).toList();

            assertThat(mutables).containsExactlyInAnyOrder("status", "billingDocumentId");
        }

        @Test
        @DisplayName("no hay ni un setter: un importe que se puede reescribir borra el pasado")
        void sin_setters() {
            List<String> setters = Arrays.stream(SubscriptionCharge.class.getDeclaredMethods())
                    .map(m -> m.getName()).filter(name -> name.startsWith("set")).toList();

            assertThat(setters).isEmpty();
        }

        @Test
        @DisplayName("sellar un cargo ya facturado es un conflicto, no una reescritura")
        void no_se_refactura() {
            SubscriptionCharge charge = persistido("179000.00");
            charge.markInvoiced(900L);

            assertThatThrownBy(() -> charge.markInvoiced(901L))
                    .isInstanceOf(SubscriptionChargeAlreadyInvoicedException.class);
            assertThat(charge.getBillingDocumentId()).isEqualTo(900L);
        }
    }

    private static SubscriptionCharge persistido(String subtotal) {
        return new SubscriptionCharge(500L, 42L, 7L, 3L, ChargeType.RECURRING, "Plan CORE agosto",
                AGOSTO, BigDecimal.ONE, new BigDecimal(subtotal), new BigDecimal(subtotal),
                new BigDecimal("19.00"), TaxTreatment.TAXED, null, ChargeStatus.PENDING, null, null,
                null, null);
    }
}
