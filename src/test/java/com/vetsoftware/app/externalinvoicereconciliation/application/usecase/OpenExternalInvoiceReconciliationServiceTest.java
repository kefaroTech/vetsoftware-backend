package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.OpenExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationAlreadyExistsException;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.externalinvoicereconciliation.testsupport.ExternalInvoiceReconciliationMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Abrir la conciliacion es lo que hace visible el problema que nadie ve.
 *
 * <p>
 * <b>El caso interesante no es el feliz, es el duplicado.</b>
 * {@code uq_eir_document} lo impediria igual, pero llegaria como una violacion
 * de indice unico: un 500 sin explicacion. La consulta previa lo convierte en
 * un 409 que dice cual es el documento — y la mitad del valor del caso es que
 * ademas <b>no escribe</b>.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenExternalInvoiceReconciliationService — abrir la ficha del devengado")
class OpenExternalInvoiceReconciliationServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-05T14:30:15Z"),
            ZoneOffset.UTC);

    @Mock
    private ExternalInvoiceReconciliationRepository repository;
    @Mock
    private BillingDocumentValidationPort billingDocumentValidationPort;

    private OpenExternalInvoiceReconciliationService service;

    @BeforeEach
    void servicio() {
        // El Clock no es un puerto: se inyecta de verdad y fijo, para que createdDate
        // sea afirmable sin depender del reloj de la maquina.
        service = new OpenExternalInvoiceReconciliationService(repository,
                billingDocumentValidationPort, RELOJ);
    }

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("abre en MISSING_EXTERNAL con los dos importes propios y la fecha del reloj")
        void abre_en_missing_external_con_los_importes_propios() {
            documentoValido();
            sinConciliacionPrevia();
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            ExternalInvoiceReconciliationDto abierta = service.execute(comando());

            assertThat(abierta.status())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL);
            assertThat(abierta.computedTotal()).isEqualByComparingTo("119000.00");
            assertThat(abierta.computedTax()).isEqualByComparingTo("19000.00");
            assertThat(abierta.externalInvoiceId()).isNull();
            assertThat(abierta.difference()).isNull();

            ArgumentCaptor<ExternalInvoiceReconciliation> guardada = ArgumentCaptor
                    .forClass(ExternalInvoiceReconciliation.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getCreatedDate())
                    .isEqualTo(LocalDateTime.of(2026, 3, 5, 14, 30, 15));
            assertThat(guardada.getValue().getCompanyId()).isEqualTo(900L);
            assertThat(guardada.getValue().getBillingDocumentId()).isEqualTo(8600L);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un documento de otra empresa no abre nada")
        void un_documento_de_otra_empresa_no_abre_nada() {
            // La FK contra subscription_billing_documents es COMPUESTA (company_id, id):
            // sin esta comprobacion la escritura llegaria a la base y saldria como un
            // error de integridad que no senala a la causa.
            when(billingDocumentValidationPort.existsByIdAndCompanyId(8600L, 900L))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Billing document not found: 8600");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("el segundo intento sobre el mismo documento sale como conflicto y no escribe")
        void el_segundo_intento_sale_como_conflicto_y_no_escribe() {
            documentoValido();
            when(repository.existsByCompanyIdAndBillingDocumentId(900L, 8600L)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(ExternalInvoiceReconciliationAlreadyExistsException.class)
                    .hasMessageContaining("billing document 8600")
                    .hasMessageContaining("company 900");

            verify(repository, never()).save(any());
        }
    }

    // --- andamio ------------------------------------------------------------

    private void documentoValido() {
        when(billingDocumentValidationPort.existsByIdAndCompanyId(8600L, 900L)).thenReturn(true);
    }

    private void sinConciliacionPrevia() {
        when(repository.existsByCompanyIdAndBillingDocumentId(900L, 8600L)).thenReturn(false);
    }

    private static OpenExternalInvoiceReconciliationCommand comando() {
        return new OpenExternalInvoiceReconciliationCommand(
                ExternalInvoiceReconciliationMother.EMPRESA,
                ExternalInvoiceReconciliationMother.DOCUMENTO, new BigDecimal("119000.00"),
                new BigDecimal("19000.00"));
    }
}
