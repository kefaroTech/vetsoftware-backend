package com.vetsoftware.app.customercredit.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.customercredit.application.command.GrantCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditAuditPort;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditBalanceRepository;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GrantCustomerCreditService")
class GrantCustomerCreditServiceTest {

    private static final Long EMPRESA = 900L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 5, 9, 0, 0);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private CustomerCreditEntryRepository entryRepository;
    @Mock
    private CustomerCreditBalanceRepository balanceRepository;
    @Mock
    private CustomerCreditAuditPort auditPort;

    private GrantCustomerCreditService service;

    @BeforeEach
    void setUp() {
        service = new GrantCustomerCreditService(entryRepository, balanceRepository, auditPort,
                RELOJ);
    }

    private GrantCustomerCreditCommand comandoValido() {
        return new GrantCustomerCreditCommand(EMPRESA, new BigDecimal("50000.00"),
                CreditOriginKind.OVERPAYMENT, 700L, null, null, LocalDate.of(2026, 6, 5), "op-1");
    }

    @Nested
    @DisplayName("Abono nuevo")
    class AbonoNuevo {

        @BeforeEach
        void sinAsientoPrevio() {
            when(entryRepository.findByCompanyIdAndClientRequestId(EMPRESA, "op-1"))
                    .thenReturn(Optional.empty());
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(balanceRepository.applyDelta(eq(EMPRESA), any(), any())).thenReturn(1);
            when(entryRepository.findOpenLotsByCompanyId(EMPRESA)).thenReturn(List.of());
        }

        @Test
        @DisplayName("abre la fila resumen antes de mover el saldo")
        void abre_la_fila_resumen_antes_de_mover_el_saldo() {
            service.execute(comandoValido());

            verify(balanceRepository).openIfAbsent(EMPRESA, AHORA);
        }

        @Test
        @DisplayName("guarda el asiento con el importe y el origen del comando")
        void guarda_el_asiento_con_el_importe_y_el_origen() {
            service.execute(comandoValido());

            ArgumentCaptor<CustomerCreditEntry> guardado = ArgumentCaptor
                    .forClass(CustomerCreditEntry.class);
            verify(entryRepository).save(guardado.capture());
            CustomerCreditEntry entry = guardado.getValue();
            assertThat(entry.getCompanyId()).isEqualTo(EMPRESA);
            assertThat(entry.getAmount()).isEqualByComparingTo("50000.00");
            assertThat(entry.getOriginKind()).isEqualTo(CreditOriginKind.OVERPAYMENT);
            assertThat(entry.getOriginPaymentId()).isEqualTo(700L);
            assertThat(entry.getExpiresOn()).isEqualTo(LocalDate.of(2026, 6, 5));
            assertThat(entry.getClientRequestId()).isEqualTo("op-1");
            assertThat(entry.getOccurredAt()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("mueve el saldo global por el mismo importe que el asiento")
        void mueve_el_saldo_global_por_el_mismo_importe() {
            service.execute(comandoValido());

            verify(balanceRepository).applyDelta(eq(EMPRESA),
                    org.mockito.ArgumentMatchers.eq(new BigDecimal("50000.00")), eq(AHORA));
        }

        @Test
        @DisplayName("audita el abono con el id, la empresa, el importe y el origen del asiento guardado")
        void audita_el_abono_con_los_datos_del_asiento_guardado() {
            service.execute(comandoValido());

            ArgumentCaptor<Long> entryId = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> companyId = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
            ArgumentCaptor<CreditOriginKind> originKind = ArgumentCaptor
                    .forClass(CreditOriginKind.class);
            ArgumentCaptor<Long> originPaymentId = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> originDocumentId = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> originSubscriptionId = ArgumentCaptor.forClass(Long.class);
            verify(auditPort).granted(entryId.capture(), companyId.capture(), amount.capture(),
                    originKind.capture(), originPaymentId.capture(), originDocumentId.capture(),
                    originSubscriptionId.capture());

            assertThat(companyId.getValue()).isEqualTo(EMPRESA);
            assertThat(amount.getValue()).isEqualByComparingTo("50000.00");
            assertThat(originKind.getValue()).isEqualTo(CreditOriginKind.OVERPAYMENT);
            assertThat(originPaymentId.getValue()).isEqualTo(700L);
            assertThat(originDocumentId.getValue()).isNull();
            assertThat(originSubscriptionId.getValue()).isNull();
        }

        @Test
        @DisplayName("devuelve el dto del asiento guardado")
        void devuelve_el_dto_del_asiento_guardado() {
            CustomerCreditEntryDto dto = service.execute(comandoValido());

            assertThat(dto.companyId()).isEqualTo(EMPRESA);
            assertThat(dto.amount()).isEqualByComparingTo("50000.00");
            assertThat(dto.originKind()).isEqualTo(CreditOriginKind.OVERPAYMENT);
        }

        @Test
        @DisplayName("recalcula la caducidad mas proxima tras mover el saldo")
        void recalcula_la_caducidad_mas_proxima() {
            service.execute(comandoValido());

            verify(balanceRepository).refreshNextExpiry(EMPRESA, null, AHORA);
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("la misma llave de cliente devuelve el asiento ya escrito sin volver a abonar")
        void la_misma_llave_devuelve_el_asiento_ya_escrito() {
            CustomerCreditEntry yaEscrito = CustomerCreditEntry.grant(EMPRESA,
                    new BigDecimal("50000.00"), CreditOriginKind.OVERPAYMENT, 700L, null, null,
                    AHORA.minusDays(1), AHORA.minusDays(1).toLocalDate(), null, "op-1",
                    AHORA.minusDays(1));
            when(entryRepository.findByCompanyIdAndClientRequestId(EMPRESA, "op-1"))
                    .thenReturn(Optional.of(yaEscrito));

            CustomerCreditEntryDto dto = service.execute(comandoValido());

            assertThat(dto.companyId()).isEqualTo(EMPRESA);
            assertThat(dto.amount()).isEqualByComparingTo("50000.00");
            verify(entryRepository, never()).save(any());
            verifyNoInteractions(balanceRepository, auditPort);
        }
    }

    @Nested
    @DisplayName("Invariante de la fila resumen")
    class InvarianteDeLaFilaResumen {

        @Test
        @DisplayName("si la fila resumen no existe tras abrirla, falla en vez de perder el abono en silencio")
        void falla_si_la_fila_resumen_no_existe_tras_abrirla() {
            when(entryRepository.findByCompanyIdAndClientRequestId(EMPRESA, "op-1"))
                    .thenReturn(Optional.empty());
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(balanceRepository.applyDelta(eq(EMPRESA), any(), any())).thenReturn(0);

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("credit balance row is missing for company " + EMPRESA);

            verify(auditPort).granted(any(), eq(EMPRESA), any(), any(), any(), any(), any());
            verify(balanceRepository, never()).refreshNextExpiry(any(), any(), any());
        }
    }
}
