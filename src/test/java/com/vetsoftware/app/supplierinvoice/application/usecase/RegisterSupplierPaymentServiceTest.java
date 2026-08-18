package com.vetsoftware.app.supplierinvoice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierinvoice.application.command.RegisterSupplierPaymentCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.InvalidSupplierInvoiceStateException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import com.vetsoftware.app.supplierinvoice.testsupport.SupplierInvoiceMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterSupplierPaymentService")
class RegisterSupplierPaymentServiceTest {

    private static final Long INVOICE_ID = 55L;
    private static final Long COMPANY_ID = 1L;
    private static final CompanyRef CO = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    private static final BranchRef BR = new BranchRef(3L, "Sede Centro");
    private static final SupplierRef SUP = new SupplierRef(7L, "Distribuidora Sur", "800111222");

    @Mock
    private SupplierInvoiceRepository repository;

    @InjectMocks
    private RegisterSupplierPaymentService service;

    @Captor
    private ArgumentCaptor<SupplierInvoice> captor;

    private SupplierInvoice facturaPendiente() {
        return SupplierInvoiceMother.nueva(CO, BR, SUP, "FAC-001", LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 2, 9), new BigDecimal("1000000"), new BigDecimal("190000"),
                new BigDecimal("25000"));
    }

    private RegisterSupplierPaymentCommand comando(BigDecimal monto) {
        return new RegisterSupplierPaymentCommand(INVOICE_ID, monto, LocalDate.of(2026, 2, 1),
                SupplierInvoicePaymentMethod.TRANSFER, "TRF-9", null, COMPANY_ID, 940L, 1L);
    }

    @Nested
    @DisplayName("registro de abonos")
    class RegistroDeAbonos {

        @Test
        @DisplayName("un abono parcial deja la factura en PARTIAL con el saldo reducido")
        void abono_parcial_deja_la_factura_en_partial() {
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(facturaPendiente()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SupplierInvoiceDto dto = service.execute(comando(new BigDecimal("165000")));

            assertThat(dto.status()).isEqualTo(SupplierInvoiceStatus.PARTIAL);
            assertThat(dto.paidAmount()).isEqualByComparingTo("165000");
            assertThat(dto.balance()).isEqualByComparingTo("1000000");
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getPayments()).hasSize(1);
        }

        @Test
        @DisplayName("un abono por el saldo total deja la factura en PAID")
        void abono_total_deja_la_factura_en_paid() {
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(facturaPendiente()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SupplierInvoiceDto dto = service.execute(comando(new BigDecimal("1165000")));

            assertThat(dto.status()).isEqualTo(SupplierInvoiceStatus.PAID);
            assertThat(dto.balance()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("factura inexistente en la empresa")
        void factura_inexistente() {
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(new BigDecimal("100"))))
                    .isInstanceOf(SupplierInvoiceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(INVOICE_ID));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un sobrepago se rechaza y no persiste nada")
        void sobrepago_se_rechaza() {
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(facturaPendiente()));

            assertThatThrownBy(() -> service.execute(comando(new BigDecimal("1165001"))))
                    .isInstanceOf(InvalidSupplierInvoiceStateException.class)
                    .hasMessageContaining("exceeds outstanding balance");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no se puede abonar una factura ya pagada")
        void no_se_puede_abonar_una_factura_pagada() {
            SupplierInvoice pagada = SupplierInvoiceMother.conEstado(CO, BR, SUP, "FAC-001",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                    new BigDecimal("190000"), new BigDecimal("25000"), SupplierInvoiceStatus.PAID,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("1165000"),
                            LocalDate.of(2026, 1, 20), SupplierInvoicePaymentMethod.CASH, null)));
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(pagada));

            assertThatThrownBy(() -> service.execute(comando(new BigDecimal("100"))))
                    .isInstanceOf(InvalidSupplierInvoiceStateException.class)
                    .hasMessageContaining("already fully paid");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no se puede abonar una factura anulada")
        void no_se_puede_abonar_una_factura_anulada() {
            SupplierInvoice anulada = SupplierInvoiceMother.conEstado(CO, BR, SUP, "FAC-001",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                    BigDecimal.ZERO, BigDecimal.ZERO, SupplierInvoiceStatus.CANCELLED, List.of());
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(anulada));

            assertThatThrownBy(() -> service.execute(comando(new BigDecimal("100"))))
                    .isInstanceOf(InvalidSupplierInvoiceStateException.class)
                    .hasMessageContaining("Cannot pay a cancelled invoice");

            verify(repository, never()).save(any());
        }
    }
}
