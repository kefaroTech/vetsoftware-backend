package com.vetsoftware.app.subscriptionpayment.domain;

import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.AHORA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.EMPRESA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.factura;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pesos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Los cuatro origenes que la capa K abrio y que este dominio rechazaba por
 * nombre, con sus barandillas.
 */
@DisplayName("BillingDocumentApplication — los cuatro origenes que saldan sin dinero")
class BillingDocumentApplicationSourcesTest {

    private static final LocalDate FECHA_VALOR = LocalDate.of(2026, 10, 30);

    @Nested
    @DisplayName("La retencion que salda")
    class Retencion {

        /**
         * <b>El caso MOR-021.</b> Ana debe 213.010, su contadora practica 7.160 de
         * retencion y le gira 205.850. Sin este camino quedan 7.160 vivos y la cuenta
         * cae a solo lectura por una deuda que fiscalmente no existe.
         */
        @Test
        @DisplayName("salda la factura sin que entre un peso y apunta a su retencion")
        void salda_sin_dinero_y_apunta_a_la_retencion() {
            BillingDocumentApplication aplicacion = BillingDocumentApplication.fromWithholding(
                    EMPRESA, factura(), 300L, pesos("7160.00"), "req-1", AHORA, FECHA_VALOR);

            assertThat(aplicacion.getSourceKind()).isEqualTo(ApplicationSourceKind.WITHHOLDING);
            assertThat(aplicacion.getWithholdingId()).isEqualTo(300L);
            assertThat(aplicacion.getPaymentId()).isNull();
            assertThat(aplicacion.getSourceDocument()).isNull();
            assertThat(aplicacion.getCreditEntryId()).isNull();
            assertThat(aplicacion.getAppliedAmount()).isEqualByComparingTo("7160.00");
        }

        /**
         * <b>La fecha valor no es la de registro.</b> Una retencion practicada el 30 de
         * octubre y registrada el 3 de noviembre pertenece a octubre; derivarla del
         * instante de registro la metia en la declaracion equivocada.
         */
        @Test
        @DisplayName("conserva la fecha valor que se le pasa, distinta del instante de registro")
        void conserva_la_fecha_valor() {
            BillingDocumentApplication aplicacion = BillingDocumentApplication.fromWithholding(
                    EMPRESA, factura(), 300L, pesos("7160.00"), null, AHORA, FECHA_VALOR);

            assertThat(aplicacion.getValueDate()).isEqualTo(FECHA_VALOR);
            assertThat(aplicacion.getValueDate()).isNotEqualTo(AHORA.toLocalDate());
        }

        @Test
        @DisplayName("exige la retencion de origen")
        void exige_la_retencion() {
            assertThatThrownBy(() -> new BillingDocumentApplication(null, EMPRESA, factura(),
                    ApplicationSourceKind.WITHHOLDING, null, null, null, null, pesos("1.00"), null,
                    null, null, null, AHORA, FECHA_VALOR, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("withholdingId is required");
        }

        @Test
        @DisplayName("prohibe apuntar ademas a un pago: seria contar el dinero dos veces")
        void prohibe_apuntar_tambien_a_un_pago() {
            assertThatThrownBy(() -> new BillingDocumentApplication(null, EMPRESA, factura(),
                    ApplicationSourceKind.WITHHOLDING, 7L, null, 300L, null, pesos("1.00"), null,
                    null, null, null, AHORA, FECHA_VALOR, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("paymentId must be null");
        }
    }

    @Nested
    @DisplayName("El saldo a favor que se aplica")
    class SaldoAFavor {

        @Test
        @DisplayName("consume de un lote concreto y no de una suma")
        void consume_de_un_lote() {
            BillingDocumentApplication aplicacion = BillingDocumentApplication.fromCustomerCredit(
                    EMPRESA, factura(), 800L, pesos("50000.00"), "req-2", AHORA, FECHA_VALOR);

            assertThat(aplicacion.getSourceKind()).isEqualTo(ApplicationSourceKind.CUSTOMER_CREDIT);
            assertThat(aplicacion.getCreditEntryId()).isEqualTo(800L);
            assertThat(aplicacion.getWithholdingId()).isNull();
        }

        @Test
        @DisplayName("exige el lote de origen")
        void exige_el_lote() {
            assertThatThrownBy(() -> new BillingDocumentApplication(null, EMPRESA, factura(),
                    ApplicationSourceKind.CUSTOMER_CREDIT, null, null, null, null, pesos("1.00"),
                    null, null, null, null, AHORA, FECHA_VALOR, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("creditEntryId is required");
        }
    }

    @Nested
    @DisplayName("El redondeo y su tope duro")
    class Redondeo {

        @ParameterizedTest(name = "admite {0} pesos de residuo")
        @ValueSource(strings = {"1.00", "2.00", "3.00", "0.01"})
        @DisplayName("admite el residuo que ningun medio de pago mueve")
        void admite_el_residuo(String importe) {
            assertThatCode(() -> BillingDocumentApplication.fromRounding(EMPRESA, factura(),
                    pesos(importe), null, AHORA, FECHA_VALOR)).doesNotThrowAnyException();
        }

        /**
         * <b>El tope es lo unico que separa este origen de un vertedero.</b> Sin el, un
         * descuadre de doscientos mil entra igual de bien que uno de dos: la cartera
         * cuadra y el error que lo produjo desaparece sin dejar rastro.
         */
        @Test
        @DisplayName("rechaza un residuo por encima del tope y dice por que")
        void rechaza_por_encima_del_tope() {
            assertThatThrownBy(() -> BillingDocumentApplication.fromRounding(EMPRESA, factura(),
                    pesos("200000.00"), null, AHORA, FECHA_VALOR))
                    .isInstanceOf(RoundingCapExceededException.class)
                    .hasMessageContaining("no es redondeo, es un descuadre");
        }

        @Test
        @DisplayName("el tope tambien acota la contra-aplicacion, medido en valor absoluto")
        void el_tope_acota_la_reversa() {
            assertThatThrownBy(() -> new BillingDocumentApplication(null, EMPRESA, factura(),
                    ApplicationSourceKind.ROUNDING, null, null, null, null, pesos("-200000.00"),
                    900L, null, null, null, AHORA, FECHA_VALOR, AHORA))
                    .isInstanceOf(RoundingCapExceededException.class);
        }

        @Test
        @DisplayName("no apunta a ninguna fila de origen: no hay ninguna que buscar")
        void no_apunta_a_nada() {
            BillingDocumentApplication aplicacion = BillingDocumentApplication.fromRounding(EMPRESA,
                    factura(), pesos("2.00"), null, AHORA, FECHA_VALOR);

            assertThat(aplicacion.getPaymentId()).isNull();
            assertThat(aplicacion.getSourceDocument()).isNull();
            assertThat(aplicacion.getWithholdingId()).isNull();
            assertThat(aplicacion.getCreditEntryId()).isNull();
        }

        @Test
        @DisplayName("el tope declarado por el dominio es el mismo que comprueba la base")
        void el_tope_es_tres() {
            assertThat(BillingDocumentApplication.MAX_ROUNDING_ABS)
                    .isEqualByComparingTo(new BigDecimal("3"));
        }
    }

    @Nested
    @DisplayName("El castigo y su firma nominal")
    class Castigo {

        @Test
        @DisplayName("da la deuda por incobrable dejando quien la autorizo y por que")
        void castiga_con_firma_y_motivo() {
            BillingDocumentApplication aplicacion = BillingDocumentApplication.fromWriteOff(EMPRESA,
                    factura(), pesos("213010.00"), 11L, "Cliente liquidado, cartera incobrable",
                    "req-3", AHORA, FECHA_VALOR);

            assertThat(aplicacion.getSourceKind()).isEqualTo(ApplicationSourceKind.WRITE_OFF);
            assertThat(aplicacion.getWriteOffAuthorizedBySystemUserId()).isEqualTo(11L);
            assertThat(aplicacion.getWriteOffReason())
                    .isEqualTo("Cliente liquidado, cartera incobrable");
        }

        @Test
        @DisplayName("sin autorizante no se escribe nada")
        void sin_autorizante_revienta() {
            assertThatThrownBy(() -> BillingDocumentApplication.fromWriteOff(EMPRESA, factura(),
                    pesos("1.00"), null, "motivo", null, AHORA, FECHA_VALOR))
                    .isInstanceOf(WriteOffSignatureRequiredException.class)
                    .hasMessageContaining("falta el usuario de plataforma");
        }

        @Test
        @DisplayName("sin motivo escrito tampoco")
        void sin_motivo_revienta() {
            assertThatThrownBy(() -> BillingDocumentApplication.fromWriteOff(EMPRESA, factura(),
                    pesos("1.00"), 11L, "   ", null, AHORA, FECHA_VALOR))
                    .isInstanceOf(WriteOffSignatureRequiredException.class)
                    .hasMessageContaining("falta el motivo escrito");
        }

        @Test
        @DisplayName("un motivo mas largo que la columna se rechaza antes de llegar a la base")
        void motivo_demasiado_largo() {
            assertThatThrownBy(() -> BillingDocumentApplication.fromWriteOff(EMPRESA, factura(),
                    pesos("1.00"), 11L, "x".repeat(256), null, AHORA, FECHA_VALOR))
                    .isInstanceOf(WriteOffSignatureRequiredException.class)
                    .hasMessageContaining("255");
        }

        /**
         * La otra mitad de {@code chk_bda_write_off_signature}: los dos campos <b>si y
         * solo si</b> el origen es el castigo. Ponerlos en otra fila afirmaria que
         * alguien autorizo algo que nadie tuvo que autorizar.
         */
        @ParameterizedTest(name = "un {0} no puede llevar firma de castigo")
        @EnumSource(value = ApplicationSourceKind.class, names = {"PAYMENT", "WITHHOLDING",
                "CUSTOMER_CREDIT", "ROUNDING"})
        @DisplayName("ningun otro origen puede llevar autorizante ni motivo")
        void ningun_otro_origen_lleva_firma(ApplicationSourceKind kind) {
            assertThatThrownBy(() -> new BillingDocumentApplication(null, EMPRESA, factura(), kind,
                    kind == ApplicationSourceKind.PAYMENT ? 7L : null, null,
                    kind == ApplicationSourceKind.WITHHOLDING ? 300L : null,
                    kind == ApplicationSourceKind.CUSTOMER_CREDIT ? 800L : null, pesos("1.00"),
                    null, 11L, "motivo", null, AHORA, FECHA_VALOR, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only a WRITE_OFF application carries");
        }
    }

    @Nested
    @DisplayName("La fecha valor")
    class FechaValor {

        @Test
        @DisplayName("es obligatoria: sin ella el asiento no cae en ningun periodo")
        void es_obligatoria() {
            assertThatThrownBy(() -> new BillingDocumentApplication(null, EMPRESA, factura(),
                    ApplicationSourceKind.ROUNDING, null, null, null, null, pesos("1.00"), null,
                    null, null, null, AHORA, null, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("valueDate is required");
        }

        @Test
        @DisplayName("la forma corta la deriva del instante de aplicacion, que es lo correcto"
                + " en un pago")
        void la_forma_corta_la_deriva() {
            BillingDocumentApplication pago = BillingDocumentApplication.fromPayment(EMPRESA,
                    factura(), 7L, pesos("100.00"), null, AHORA);

            assertThat(pago.getValueDate()).isEqualTo(AHORA.toLocalDate());
        }
    }

    @Nested
    @DisplayName("La contra-aplicacion conserva el origen")
    class Reversa {

        @Test
        @DisplayName("una reversa de retencion sigue apuntando a su retencion")
        void reversa_de_retencion_conserva_la_referencia() {
            BillingDocumentApplication original = new BillingDocumentApplication(500L, EMPRESA,
                    factura(), ApplicationSourceKind.WITHHOLDING, null, null, 300L, null,
                    pesos("7160.00"), null, null, null, "req-1", AHORA, FECHA_VALOR, AHORA);

            BillingDocumentApplication reversa = BillingDocumentApplication.reversalOf(original,
                    AHORA);

            assertThat(reversa.getWithholdingId()).isEqualTo(300L);
            assertThat(reversa.getAppliedAmount()).isEqualByComparingTo("-7160.00");
            assertThat(reversa.getClientRequestId()).isNull();
        }

        @Test
        @DisplayName("una reversa de castigo conserva la firma que deshace")
        void reversa_de_castigo_conserva_la_firma() {
            BillingDocumentApplication original = new BillingDocumentApplication(501L, EMPRESA,
                    factura(), ApplicationSourceKind.WRITE_OFF, null, null, null, null,
                    pesos("1000.00"), null, 11L, "incobrable", null, AHORA, FECHA_VALOR, AHORA);

            BillingDocumentApplication reversa = BillingDocumentApplication.reversalOf(original,
                    AHORA);

            assertThat(reversa.getWriteOffAuthorizedBySystemUserId()).isEqualTo(11L);
            assertThat(reversa.getWriteOffReason()).isEqualTo("incobrable");
        }

        @Test
        @DisplayName("una reversa de saldo a favor conserva su lote")
        void reversa_de_credito_conserva_el_lote() {
            BillingDocumentApplication original = new BillingDocumentApplication(502L, EMPRESA,
                    factura(), ApplicationSourceKind.CUSTOMER_CREDIT, null, null, null, 800L,
                    pesos("500.00"), null, null, null, null, AHORA, FECHA_VALOR, AHORA);

            assertThat(BillingDocumentApplication.reversalOf(original, AHORA).getCreditEntryId())
                    .isEqualTo(800L);
        }
    }
}
