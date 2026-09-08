package com.vetsoftware.app.quote.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.quote.application.command.PurchaseModulesCommand;
import com.vetsoftware.app.quote.application.dto.PurchaseModulesResultDto;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.SelfServeQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.EmployeeAdminCheckPort;
import com.vetsoftware.app.quote.application.port.out.EmployeeEmailQueryPort;
import com.vetsoftware.app.quote.application.port.out.ImmediatePaidLineOpeningPort;
import com.vetsoftware.app.quote.application.port.out.ModuleLineSuccessionPort;
import com.vetsoftware.app.quote.application.port.out.ModuleLineSuccessionPort.CurrentLine;
import com.vetsoftware.app.quote.application.port.out.PaymentSourceDefaultingPort;
import com.vetsoftware.app.quote.application.port.out.QuoteAuditPort;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.testsupport.QuoteMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PurchaseModulesServiceTest {

    private static final Long COMPANY_ID = QuoteMother.empresa().id();
    private static final Long EMPLOYEE_ID = 4L;
    private static final Long QUOTE_ID = 77L;
    private static final Long SUBSCRIPTION_ID = 501L;
    private static final Long CATALOG_ITEM_ID = QuoteMother.modulo().id();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private SelfServeQuoteUseCase selfServeQuoteUseCase;
    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private QuoteAuditPort audit;
    @Mock
    private EmployeeAdminCheckPort adminCheckPort;
    @Mock
    private EmployeeEmailQueryPort employeeEmailQueryPort;
    @Mock
    private ModuleLineSuccessionPort successionPort;
    @Mock
    private ImmediatePaidLineOpeningPort immediateOpeningPort;
    @Mock
    private PaymentSourceDefaultingPort paymentSourceDefaultingPort;

    private PurchaseModulesService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseModulesService(selfServeQuoteUseCase, quoteRepository, audit,
                adminCheckPort, employeeEmailQueryPort, successionPort, immediateOpeningPort,
                paymentSourceDefaultingPort, CLOCK);
    }

    private static PurchaseModulesCommand comandoValido() {
        return new PurchaseModulesCommand(COMPANY_ID, EMPLOYEE_ID, java.util.List.of("GROOMING"),
                "MONTHLY", 12L, "req-purchase-1", "203.0.113.7");
    }

    private void mockQuoteIssuedAndAcceptable() {
        Quote issued = QuoteMother.persistida(QUOTE_ID, QuoteStatus.SENT);
        when(selfServeQuoteUseCase.execute(any())).thenReturn(QuoteDto.from(issued));
        when(quoteRepository.findByIdAndCompanyId(QUOTE_ID, COMPANY_ID))
                .thenReturn(Optional.of(QuoteMother.persistida(QUOTE_ID, QuoteStatus.SENT)));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeEmailQueryPort.findEmail(EMPLOYEE_ID, COMPANY_ID))
                .thenReturn(Optional.of("admin@vetsoftware.test"));
        when(adminCheckPort.isCompanyAdmin(EMPLOYEE_ID, COMPANY_ID)).thenReturn(true);
        when(successionPort.findCurrentSubscriptionId(COMPANY_ID))
                .thenReturn(Optional.of(SUBSCRIPTION_ID));
    }

    @Nested
    @DisplayName("Autorización — el rol base ADMIN, no el permiso solo")
    class Autorizacion {

        @Test
        @DisplayName("un empleado sin rol ADMIN recibe 403 y no toca ningún puerto de escritura")
        void empleado_sin_rol_admin_es_rechazado() {
            when(adminCheckPort.isCompanyAdmin(EMPLOYEE_ID, COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(selfServeQuoteUseCase, quoteRepository, successionPort,
                    immediateOpeningPort, paymentSourceDefaultingPort);
        }
    }

    @Nested
    @DisplayName("Línea vigente TRIAL — sucesión futura, sin cobro (DO-1)")
    class SucesionDeTrial {

        @Test
        @DisplayName("cierra la prueba en trialEnd+1 y abre la línea PAID esa misma fecha, sin cobrar hoy")
        void sucede_la_trial_sin_cobrar_hoy() {
            mockQuoteIssuedAndAcceptable();
            LocalDate trialEnd = LocalDate.of(2026, 9, 30);
            when(successionPort.findCurrentLine(COMPANY_ID, CATALOG_ITEM_ID)).thenReturn(
                    Optional.of(new CurrentLine(900L, SUBSCRIPTION_ID, "TRIAL", trialEnd)));

            PurchaseModulesResultDto result = service.execute(comandoValido());

            LocalDate expectedFirstCharge = trialEnd.plusDays(1);
            ArgumentCaptor<LocalDate> closeDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> effectiveFromCaptor = ArgumentCaptor
                    .forClass(LocalDate.class);
            verify(successionPort).succeedLine(eq(COMPANY_ID), eq(SUBSCRIPTION_ID), eq(900L),
                    closeDateCaptor.capture(), effectiveFromCaptor.capture(), any());
            assertThat(closeDateCaptor.getValue()).isEqualTo(expectedFirstCharge);
            assertThat(effectiveFromCaptor.getValue()).isEqualTo(expectedFirstCharge);
            assertThat(result.lines()).hasSize(1);
            assertThat(result.lines().get(0).firstChargeDate()).isEqualTo(expectedFirstCharge);
            assertThat(result.lines().get(0).chargedNow()).isFalse();
            verifyNoInteractions(immediateOpeningPort);
        }
    }

    @Nested
    @DisplayName("Sin línea previa (NEVER_FREE) — el otrosí devenga hoy, pero no cobra hoy")
    class CompraNeverFree {

        @Test
        @DisplayName("abre la línea hoy sin cerrar nada, y reporta el próximo corte de ciclo como primer cobro")
        void abre_la_linea_hoy_y_reporta_el_proximo_corte_como_primer_cobro() {
            mockQuoteIssuedAndAcceptable();
            when(successionPort.findCurrentLine(COMPANY_ID, CATALOG_ITEM_ID))
                    .thenReturn(Optional.empty());
            LocalDate proximoCorte = LocalDate.of(2026, 10, 1);
            when(successionPort.findNextBillingDate(COMPANY_ID))
                    .thenReturn(Optional.of(proximoCorte));

            PurchaseModulesResultDto result = service.execute(comandoValido());

            LocalDate today = LocalDate.now(CLOCK);
            verify(immediateOpeningPort).openNow(eq(COMPANY_ID), eq(SUBSCRIPTION_ID),
                    eq(CATALOG_ITEM_ID), eq(EMPLOYEE_ID), any(), any(), eq(today));
            verify(successionPort, never()).closeLine(anyLong(), anyLong(), any());
            verify(successionPort, never()).succeedLine(any(), any(), any(), any(), any(), any());
            assertThat(result.lines().get(0).firstChargeDate()).isEqualTo(proximoCorte);
            assertThat(result.lines().get(0).chargedNow()).isFalse();
        }

        @Test
        @DisplayName("si el corte de ciclo cae hoy mismo, chargedNow sí es verdad")
        void chargedNow_es_verdad_solo_si_el_corte_cae_hoy() {
            mockQuoteIssuedAndAcceptable();
            when(successionPort.findCurrentLine(COMPANY_ID, CATALOG_ITEM_ID))
                    .thenReturn(Optional.empty());
            LocalDate today = LocalDate.now(CLOCK);
            when(successionPort.findNextBillingDate(COMPANY_ID)).thenReturn(Optional.of(today));

            PurchaseModulesResultDto result = service.execute(comandoValido());

            assertThat(result.lines().get(0).firstChargeDate()).isEqualTo(today);
            assertThat(result.lines().get(0).chargedNow()).isTrue();
        }
    }

    @Nested
    @DisplayName("Prueba ya vencida (FREE_LIMITED/EXPIRED_READ_ONLY) — cierra hoy, cobra en el próximo corte")
    class CompraTrasVencer {

        @Test
        @DisplayName("cierra la línea degradada hoy y abre la de pago por el mismo otrosí")
        void cierra_hoy_y_reporta_el_proximo_corte() {
            mockQuoteIssuedAndAcceptable();
            when(successionPort.findCurrentLine(COMPANY_ID, CATALOG_ITEM_ID)).thenReturn(
                    Optional.of(new CurrentLine(901L, SUBSCRIPTION_ID, "FREE_LIMITED", null)));
            LocalDate proximoCorte = LocalDate.of(2026, 10, 1);
            when(successionPort.findNextBillingDate(COMPANY_ID))
                    .thenReturn(Optional.of(proximoCorte));

            PurchaseModulesResultDto result = service.execute(comandoValido());

            LocalDate today = LocalDate.now(CLOCK);
            verify(successionPort).closeLine(COMPANY_ID, 901L, today);
            verify(immediateOpeningPort).openNow(eq(COMPANY_ID), eq(SUBSCRIPTION_ID),
                    eq(CATALOG_ITEM_ID), eq(EMPLOYEE_ID), any(), any(), eq(today));
            assertThat(result.lines().get(0).firstChargeDate()).isEqualTo(proximoCorte);
            assertThat(result.lines().get(0).chargedNow()).isFalse();
        }
    }

    @Nested
    @DisplayName("Medio de pago")
    class MedioDePago {

        @Test
        @DisplayName("marca el medio de pago recibido como predeterminado")
        void marca_el_medio_de_pago_como_predeterminado() {
            mockQuoteIssuedAndAcceptable();
            when(successionPort.findCurrentLine(COMPANY_ID, CATALOG_ITEM_ID))
                    .thenReturn(Optional.empty());

            service.execute(comandoValido());

            verify(paymentSourceDefaultingPort).markDefaultIfNeeded(COMPANY_ID, 12L);
        }
    }
}
