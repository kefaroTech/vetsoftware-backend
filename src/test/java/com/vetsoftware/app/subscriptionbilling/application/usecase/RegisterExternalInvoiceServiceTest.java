package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.RegisterExternalInvoiceCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingPolicyPort;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentAlreadyIssuedException;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.ExternalInvoiceReference;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterExternalInvoiceService — el vencimiento sale de la fecha fiscal")
class RegisterExternalInvoiceServiceTest {

    private static final Long EMPRESA = 42L;
    /**
     * El registro ocurre el 25 de septiembre; la factura se emitio fuera el 20 de
     * agosto.
     */
    private static final Clock RELOJ_DEL_REGISTRO = Clock
            .fixed(Instant.parse("2026-09-25T14:00:00Z"), ZoneId.of("America/Bogota"));
    /** El documento se calculo aqui el 1 de agosto. */
    private static final LocalDateTime CALCULADO_EL = LocalDateTime.of(2026, 8, 1, 7, 0);
    private static final LocalDate FECHA_FISCAL = LocalDate.of(2026, 8, 20);

    @Mock
    private BillingDocumentRepository repository;
    @Mock
    private BillingPolicyPort billingPolicyPort;

    private RegisterExternalInvoiceService service;

    @BeforeEach
    void setUp() {
        service = new RegisterExternalInvoiceService(repository, billingPolicyPort,
                RELOJ_DEL_REGISTRO);
    }

    private static SubscriptionBillingDocument documento(IssueStatus status,
            ExternalInvoiceReference external) {
        return new SubscriptionBillingDocument(900L, "DC-000001", EMPRESA, 7L, DocumentKind.INVOICE,
                BillingReason.RECURRING_CYCLE,
                new ServicePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)), status,
                external, null, external == null ? null : FECHA_FISCAL.plusDays(15),
                new BigDecimal("100000.00"), new BigDecimal("19000.00"),
                new BigDecimal("119000.00"), BigDecimal.ZERO, List.of(), CALCULADO_EL, 0L);
    }

    private RegisterExternalInvoiceCommand comando() {
        return new RegisterExternalInvoiceCommand(900L, EMPRESA, "FE-4711", "CUFE123", FECHA_FISCAL,
                "SIIGO", 3L);
    }

    @Nested
    @DisplayName("Vencimiento — TRAMPA 4")
    class Vencimiento {

        @Test
        @DisplayName("se cuenta desde external_issued_at, NO desde la fecha de calculo interno"
                + " ni desde el momento del registro")
        void desde_la_fecha_fiscal_y_no_desde_ninguna_otra() {
            when(repository.findByIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(Optional.of(documento(IssueStatus.AWAITING_EXTERNAL, null)));
            when(billingPolicyPort.defaultPaymentTermDays()).thenReturn(15);
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            BillingDocumentDto dto = service.execute(comando());

            assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 9, 4));
            // Ni desde el calculo interno (1-ago + 15 = 16-ago, ya vencida al
            // registrarla) ni desde el momento del registro (25-sep + 15 = 10-oct).
            assertThat(dto.dueDate()).isNotEqualTo(CALCULADO_EL.toLocalDate().plusDays(15))
                    .isNotEqualTo(LocalDate.of(2026, 10, 10));
        }

        @Test
        @DisplayName("el reloj solo sella cuando se capturo la referencia aqui,"
                + " que es el rastro del paso manual")
        void el_reloj_solo_sella_el_registro() {
            when(repository.findByIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(Optional.of(documento(IssueStatus.AWAITING_EXTERNAL, null)));
            when(billingPolicyPort.defaultPaymentTermDays()).thenReturn(15);
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            BillingDocumentDto dto = service.execute(comando());

            assertThat(dto.externalRegisteredAt()).isEqualTo(LocalDateTime.of(2026, 9, 25, 9, 0));
            assertThat(dto.externalIssuedAt()).isEqualTo(FECHA_FISCAL);
            assertThat(dto.externalRegisteredBySystemUserId()).isEqualTo(3L);
            assertThat(dto.issueStatus()).isEqualTo(IssueStatus.EXTERNAL_REGISTERED);
        }
    }

    @Nested
    @DisplayName("La factura externa sella el importe — TRAMPA 5")
    class YaEmitido {

        @Test
        @DisplayName("un documento ya registrado no admite otra factura externa,"
                + " y no se guarda nada")
        void no_se_registra_dos_veces() {
            ExternalInvoiceReference yaRegistrada = new ExternalInvoiceReference("FE-1", null,
                    FECHA_FISCAL, "SIIGO", LocalDateTime.of(2026, 8, 21, 9, 0), 3L);
            when(repository.findByIdAndCompanyId(900L, EMPRESA)).thenReturn(
                    Optional.of(documento(IssueStatus.EXTERNAL_REGISTERED, yaRegistrada)));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(BillingDocumentAlreadyIssuedException.class)
                    .hasMessageContaining("credit note");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("un documento de otra empresa no existe para este caso de uso")
        void documento_ajeno_no_existe() {
            when(repository.findByIdAndCompanyId(900L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(SubscriptionBillingDocumentNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }
}
