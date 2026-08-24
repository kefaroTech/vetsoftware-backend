package com.vetsoftware.app.subscriptionpayment.domain;

import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.AHORA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.EMPRESA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.aplicacionDePago;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.factura;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.facturaDeOtraEmpresa;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.notaCredito;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pesos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BillingDocumentApplicationTest {

    @Nested
    @DisplayName("Creacion desde un pago")
    class DesdePago {

        @Test
        @DisplayName("una aplicacion de pago nace positiva y sin documento origen")
        void nace_positiva_sin_documento_origen() {
            BillingDocumentApplication application = BillingDocumentApplication.fromPayment(EMPRESA,
                    factura(), 7L, pesos("200000.00"), "req-1", AHORA);

            assertThat(application.getSourceKind()).isEqualTo(ApplicationSourceKind.PAYMENT);
            assertThat(application.getPaymentId()).isEqualTo(7L);
            assertThat(application.getSourceDocument()).isNull();
            assertThat(application.getAppliedAmount()).isEqualByComparingTo("200000.00");
            assertThat(application.isReversal()).isFalse();
        }
    }

    @Nested
    @DisplayName("Creacion desde una nota credito")
    class DesdeNotaCredito {

        @Test
        @DisplayName("una nota credito salda la factura sin que entre un peso")
        void la_nota_credito_salda_sin_dinero() {
            BillingDocumentApplication application = BillingDocumentApplication.fromCreditNote(
                    EMPRESA, factura(), notaCredito(), pesos("300000.00"), "req-1", AHORA);

            assertThat(application.getSourceKind()).isEqualTo(ApplicationSourceKind.CREDIT_NOTE);
            assertThat(application.getPaymentId()).isNull();
            assertThat(application.getSourceDocument().documentNumber()).isEqualTo("NC-2026-0001");
            assertThat(application.getAppliedAmount()).isEqualByComparingTo("300000.00");
        }

        @Test
        @DisplayName("el documento origen tiene que ser una nota credito, no otra factura")
        void el_origen_debe_ser_una_nota_credito() {
            BillingDocumentRef otraFactura = new BillingDocumentRef(300L, EMPRESA, "FAC-2026-0009",
                    "INVOICE", pesos("50000.00"), pesos("50000.00"));

            assertThatThrownBy(() -> BillingDocumentApplication.fromCreditNote(EMPRESA, factura(),
                    otraFactura, pesos("10000.00"), null, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a CREDIT_NOTE");
        }

        @Test
        @DisplayName("un documento no se puede saldar consigo mismo")
        void no_se_salda_a_si_mismo() {
            BillingDocumentRef notaCredito = notaCredito();

            assertThatThrownBy(() -> BillingDocumentApplication.fromCreditNote(EMPRESA, notaCredito,
                    notaCredito, pesos("1000.00"), null, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot settle itself");
        }
    }

    @Nested
    @DisplayName("Exclusividad del origen")
    class ExclusividadDelOrigen {

        @Test
        @DisplayName("un origen PAYMENT sin pago no reduce el saldo desde ninguna parte")
        void payment_exige_pago() {
            assertThatThrownBy(
                    () -> nueva(ApplicationSourceKind.PAYMENT, null, null, pesos("100.00"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("paymentId is required");
        }

        @Test
        @DisplayName("un origen PAYMENT no puede traer ademas un documento origen")
        void payment_prohibe_documento() {
            assertThatThrownBy(() -> nueva(ApplicationSourceKind.PAYMENT, 7L, notaCredito(),
                    pesos("100.00"), null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceDocument must be null");
        }

        @Test
        @DisplayName("un origen CREDIT_NOTE sin documento no dice de donde sale el credito")
        void credito_exige_documento() {
            assertThatThrownBy(() -> nueva(ApplicationSourceKind.CREDIT_NOTE, null, null,
                    pesos("100.00"), null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceDocument is required");
        }

        @Test
        @DisplayName("un origen CREDIT_NOTE no puede traer ademas un pago")
        void credito_prohibe_pago() {
            assertThatThrownBy(() -> nueva(ApplicationSourceKind.CREDIT_NOTE, 7L, notaCredito(),
                    pesos("100.00"), null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("paymentId must be null");
        }
    }

    @Nested
    @DisplayName("Signo y reversas")
    class SignoYReversas {

        @Test
        @DisplayName("una aplicacion normal no puede ser negativa")
        void aplicacion_normal_es_positiva() {
            assertThatThrownBy(
                    () -> nueva(ApplicationSourceKind.PAYMENT, 7L, null, pesos("-100.00"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be positive");
        }

        @Test
        @DisplayName("una reversa no puede ser positiva")
        void reversa_es_negativa() {
            assertThatThrownBy(
                    () -> nueva(ApplicationSourceKind.PAYMENT, 7L, null, pesos("100.00"), 500L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reversal must be negative");
        }

        @Test
        @DisplayName("rechaza una llave de idempotencia que no cabe en la columna")
        void rechaza_llave_larga() {
            assertThatThrownBy(() -> new BillingDocumentApplication(null, EMPRESA, factura(),
                    ApplicationSourceKind.PAYMENT, 7L, null, pesos("100.00"), null, "x".repeat(65),
                    AHORA, AHORA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("clientRequestId must be 64 chars or less");
        }

        @Test
        @DisplayName("un importe cero no es una aplicacion, es ruido")
        void rechaza_importe_cero() {
            assertThatThrownBy(
                    () -> nueva(ApplicationSourceKind.PAYMENT, 7L, null, BigDecimal.ZERO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be zero");
        }

        @Test
        @DisplayName("la reversa conserva factura y origen, y niega el importe")
        void la_reversa_niega_el_importe() {
            BillingDocumentApplication original = aplicacionDePago();

            BillingDocumentApplication reversa = BillingDocumentApplication.reversalOf(original,
                    AHORA);

            assertThat(reversa.getAppliedAmount()).isEqualByComparingTo("-200000.00");
            assertThat(reversa.getReversalOfId()).isEqualTo(original.getId());
            assertThat(reversa.getTargetDocument()).isEqualTo(original.getTargetDocument());
            assertThat(reversa.getPaymentId()).isEqualTo(original.getPaymentId());
            assertThat(reversa.isReversal()).isTrue();
        }

        @Test
        @DisplayName("la suma neta de una aplicacion y su reversa es cero, y libera el origen")
        void aplicacion_mas_reversa_suman_cero() {
            BillingDocumentApplication original = aplicacionDePago();
            BillingDocumentApplication reversa = BillingDocumentApplication.reversalOf(original,
                    AHORA);

            assertThat(original.getAppliedAmount().add(reversa.getAppliedAmount()))
                    .isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("la reversa no hereda la llave de la original: chocaria con su unica")
        void la_reversa_no_hereda_la_llave() {
            BillingDocumentApplication original = aplicacionDePago();

            BillingDocumentApplication reversa = BillingDocumentApplication.reversalOf(original,
                    AHORA);

            assertThat(original.getClientRequestId()).isNotNull();
            assertThat(reversa.getClientRequestId()).isNull();
        }

        @Test
        @DisplayName("una reversa no se puede revertir")
        void no_se_revierte_una_reversa() {
            BillingDocumentApplication reversa = BillingDocumentApplication
                    .reversalOf(aplicacionDePago(), AHORA);

            assertThatThrownBy(() -> BillingDocumentApplication.reversalOf(reversa, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a reversal cannot be reversed");
        }

        @Test
        @DisplayName("no se revierte una aplicacion que todavia no esta persistida")
        void no_se_revierte_lo_no_persistido() {
            BillingDocumentApplication sinId = BillingDocumentApplication.fromPayment(EMPRESA,
                    factura(), 7L, pesos("100.00"), null, AHORA);

            assertThatThrownBy(() -> BillingDocumentApplication.reversalOf(sinId, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be persisted");
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("un pago de una clinica no puede saldar la factura de otra")
        void no_salda_la_factura_de_otra_empresa() {
            assertThatThrownBy(() -> BillingDocumentApplication.fromPayment(EMPRESA,
                    facturaDeOtraEmpresa(), 7L, pesos("100.00"), null, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("targetDocument belongs to another company");
        }

        @Test
        @DisplayName("una nota credito de otra clinica no puede saldar una factura propia")
        void no_aplica_la_nota_credito_de_otra_empresa() {
            BillingDocumentRef notaAjena = new BillingDocumentRef(201L, 99L, "NC-2026-0009",
                    "CREDIT_NOTE", pesos("100000.00"), pesos("0.00"));

            assertThatThrownBy(() -> BillingDocumentApplication.fromCreditNote(EMPRESA, factura(),
                    notaAjena, pesos("100.00"), null, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceDocument belongs to another company");
        }
    }

    private static BillingDocumentApplication nueva(ApplicationSourceKind sourceKind,
            Long paymentId, BillingDocumentRef sourceDocument, BigDecimal appliedAmount,
            Long reversalOfId) {
        return new BillingDocumentApplication(null, EMPRESA, factura(), sourceKind, paymentId,
                sourceDocument, appliedAmount, reversalOfId, null, AHORA, AHORA);
    }
}
