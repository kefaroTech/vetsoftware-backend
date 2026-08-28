package com.vetsoftware.app.paymentattempt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentattempt.application.command.RecordPaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.paymentattempt.application.port.out.PaymentAttemptRepository;
import com.vetsoftware.app.paymentattempt.application.port.out.SubscriptionPaymentMethodValidationPort;
import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.domain.PaymentAttempt;
import com.vetsoftware.app.paymentattempt.domain.RetryBudgetExhaustedException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Cuatro intentos en dos semanas, y la exencion de los errores propios.</b>
 *
 * <p>
 * La rodaja de persistencia comprueba que el contador excluye los
 * {@link DeclineKind#CONFIGURATION} y respeta la ventana; lo que solo se ve
 * aqui es la <b>decision</b> que se toma con ese numero: cuando se corta, con
 * que corte se pregunta, y —lo mas importante— que un fallo propio <b>ni
 * siquiera pregunta</b>. Que no pregunte no es una optimizacion: si preguntara
 * y comparara, un cliente con el presupuesto agotado no podria ni registrar que
 * la pasarela se cayo, y el expediente perderia justo la prueba de que el
 * impago no era suyo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecordPaymentAttemptService — el presupuesto de reintentos y su exencion")
class RecordPaymentAttemptServiceTest {

    private static final Long EMPRESA = 900L;
    private static final Long DOCUMENTO = 8400L;
    private static final Long MEDIO_DE_PAGO = 8410L;

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 20, 12, 0, 0);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private PaymentAttemptRepository repository;
    @Mock
    private BillingDocumentValidationPort billingDocumentValidationPort;
    @Mock
    private SubscriptionPaymentMethodValidationPort paymentMethodValidationPort;

    private RecordPaymentAttemptService service;

    @BeforeEach
    void servicio() {
        service = new RecordPaymentAttemptService(repository, billingDocumentValidationPort,
                paymentMethodValidationPort, RELOJ);
    }

    @Nested
    @DisplayName("Presupuesto del cliente")
    class PresupuestoDelCliente {

        @Test
        @DisplayName("con el techo alcanzado rechaza el intento y no escribe nada")
        void con_el_techo_alcanzado_rechaza_y_no_escribe() {
            elDocumentoExiste();
            elMedioExiste();
            elConsecutivoVaPor(4);
            when(repository.countRetryableSince(anyLong(), anyLong(), any()))
                    .thenReturn(PaymentAttempt.MAX_SOFT_ATTEMPTS);

            assertThatThrownBy(() -> service.execute(comando(DeclineKind.SOFT, MEDIO_DE_PAGO)))
                    .isInstanceOf(RetryBudgetExhaustedException.class).hasMessageContaining(
                            "Retry budget exhausted for billing document " + DOCUMENTO);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("con un intento libre todavia lo registra, y con el consecutivo siguiente")
        void con_un_intento_libre_todavia_lo_registra() {
            elDocumentoExiste();
            elMedioExiste();
            elConsecutivoVaPor(3);
            when(repository.countRetryableSince(anyLong(), anyLong(), any()))
                    .thenReturn(PaymentAttempt.MAX_SOFT_ATTEMPTS - 1);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando(DeclineKind.SOFT, MEDIO_DE_PAGO));

            ArgumentCaptor<PaymentAttempt> guardado = ArgumentCaptor.forClass(PaymentAttempt.class);
            verify(repository).save(guardado.capture());
            // El consecutivo lo calcula el caso de uso dentro de la transaccion porque
            // uq_payment_attempts_number no admite dos iguales por documento.
            assertThat(guardado.getValue().getAttemptNumber()).isEqualTo(4);
            assertThat(guardado.getValue().getCreatedDate()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("el primer intento de un documento arranca en uno")
        void el_primer_intento_arranca_en_uno() {
            elDocumentoExiste();
            elMedioExiste();
            when(repository.findMaxAttemptNumber(EMPRESA, DOCUMENTO)).thenReturn(Optional.empty());
            when(repository.countRetryableSince(anyLong(), anyLong(), any())).thenReturn(0);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando(DeclineKind.SOFT, MEDIO_DE_PAGO));

            ArgumentCaptor<PaymentAttempt> guardado = ArgumentCaptor.forClass(PaymentAttempt.class);
            verify(repository).save(guardado.capture());
            // Cero mas uno, no cero: chk_payment_attempts_number exige > 0.
            assertThat(guardado.getValue().getAttemptNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("pregunta por la ventana de dos semanas contada desde el reloj inyectado")
        void pregunta_por_la_ventana_de_dos_semanas() {
            elDocumentoExiste();
            elMedioExiste();
            elConsecutivoVaPor(1);
            when(repository.countRetryableSince(anyLong(), anyLong(), any())).thenReturn(1);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando(DeclineKind.SOFT, MEDIO_DE_PAGO));

            ArgumentCaptor<LocalDateTime> corte = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(repository).countRetryableSince(anyLong(), anyLong(), corte.capture());
            // Catorce dias exactos hacia atras. Con un now() pelado este valor seria
            // irreproducible; con el Clock inyectado se puede afirmar al segundo, y
            // cambiar la ventana sin querer pone el caso rojo.
            assertThat(corte.getValue()).isEqualTo(AHORA.minusDays(14));
            assertThat(PaymentAttempt.RETRY_WINDOW.toDays()).isEqualTo(14L);
        }
    }

    @Nested
    @DisplayName("Exencion de los errores propios")
    class ExencionDeLosErroresPropios {

        @Test
        @DisplayName("un fallo propio ni siquiera consulta el contador y se registra igual")
        void un_fallo_propio_ni_consulta_el_contador() {
            elDocumentoExiste();
            when(repository.findMaxAttemptNumber(EMPRESA, DOCUMENTO)).thenReturn(Optional.of(4));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            // Cuatro intentos ya gastados —el techo— y aun asi entra: la credencial
            // mal puesta o la pasarela caida no son culpa del cliente y no arrancan
            // cobranza contra el. Si el servicio preguntara al contador, el stub que
            // falta haria fallar la llamada bajo STRICT_STUBS.
            service.execute(comando(DeclineKind.CONFIGURATION, null));

            ArgumentCaptor<PaymentAttempt> guardado = ArgumentCaptor.forClass(PaymentAttempt.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getDeclineKind()).isEqualTo(DeclineKind.CONFIGURATION);
            assertThat(guardado.getValue().consumesCustomerAttempts()).isFalse();
            verify(repository, never()).countRetryableSince(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("un fallo propio sin medio de pago no consulta el puerto de medios")
        void un_fallo_propio_sin_medio_no_consulta_el_puerto_de_medios() {
            elDocumentoExiste();
            when(repository.findMaxAttemptNumber(EMPRESA, DOCUMENTO)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando(DeclineKind.CONFIGURATION, null));

            // Exigir el medio obligaria a inventarse uno para el rebote que ocurre
            // antes de llegar a usarlo.
            verifyNoInteractions(paymentMethodValidationPort);
        }

        @Test
        @DisplayName("un rechazo duro si gasta presupuesto y llega al contador")
        void un_rechazo_duro_si_gasta_presupuesto() {
            elDocumentoExiste();
            elMedioExiste();
            elConsecutivoVaPor(4);
            when(repository.countRetryableSince(anyLong(), anyLong(), any()))
                    .thenReturn(PaymentAttempt.MAX_SOFT_ATTEMPTS);

            // Solo CONFIGURATION esta exento. Un HARD es del cliente y cuenta, aunque
            // no se vuelva a reintentar.
            assertThatThrownBy(() -> service.execute(comando(DeclineKind.HARD, MEDIO_DE_PAGO)))
                    .isInstanceOf(RetryBudgetExhaustedException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un documento de otra empresa aborta antes de pedir el consecutivo")
        void un_documento_de_otra_empresa_aborta_antes_del_consecutivo() {
            when(billingDocumentValidationPort.existsByIdAndCompanyId(DOCUMENTO, EMPRESA))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(comando(DeclineKind.SOFT, MEDIO_DE_PAGO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Billing document not found: " + DOCUMENTO);

            verifyNoInteractions(repository);
            verifyNoInteractions(paymentMethodValidationPort);
        }

        @Test
        @DisplayName("un medio de pago de otra empresa aborta y no escribe nada")
        void un_medio_de_pago_de_otra_empresa_aborta() {
            elDocumentoExiste();
            when(paymentMethodValidationPort.existsByIdAndCompanyId(MEDIO_DE_PAGO, EMPRESA))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(comando(DeclineKind.SOFT, MEDIO_DE_PAGO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Payment method not found: " + MEDIO_DE_PAGO);

            verifyNoInteractions(repository);
        }
    }

    // --- andamio ------------------------------------------------------------

    private void elDocumentoExiste() {
        when(billingDocumentValidationPort.existsByIdAndCompanyId(DOCUMENTO, EMPRESA))
                .thenReturn(true);
    }

    private void elMedioExiste() {
        when(paymentMethodValidationPort.existsByIdAndCompanyId(MEDIO_DE_PAGO, EMPRESA))
                .thenReturn(true);
    }

    private void elConsecutivoVaPor(int ultimo) {
        when(repository.findMaxAttemptNumber(EMPRESA, DOCUMENTO)).thenReturn(Optional.of(ultimo));
    }

    private static RecordPaymentAttemptCommand comando(DeclineKind clase, Long medioDePago) {
        String codigo = clase == DeclineKind.CONFIGURATION ? null : "insufficient_funds";
        LocalDateTime siguiente = clase == DeclineKind.SOFT ? AHORA.plusDays(3) : null;
        return new RecordPaymentAttemptCommand(EMPRESA, DOCUMENTO, medioDePago, "wompi",
                new BigDecimal("119000.00"), codigo, clase, AHORA.minusMinutes(5), siguiente);
    }
}
