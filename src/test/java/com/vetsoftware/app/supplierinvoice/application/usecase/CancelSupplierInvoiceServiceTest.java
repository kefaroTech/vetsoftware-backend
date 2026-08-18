package com.vetsoftware.app.supplierinvoice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("CancelSupplierInvoiceService")
class CancelSupplierInvoiceServiceTest {

    private static final Long INVOICE_ID = 55L;
    private static final Long COMPANY_ID = 1L;
    private static final Long ACTOR_ID = 940L;
    private static final CompanyRef CO = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    private static final BranchRef BR = new BranchRef(3L, "Sede Centro");
    private static final SupplierRef SUP = new SupplierRef(7L, "Distribuidora Sur", "800111222");

    @Mock
    private SupplierInvoiceRepository repository;

    @InjectMocks
    private CancelSupplierInvoiceService service;

    @Captor
    private ArgumentCaptor<SupplierInvoice> captor;

    @Nested
    @DisplayName("anulacion")
    class Anulacion {

        @Test
        @DisplayName("una factura PENDING se anula y se persiste")
        void una_factura_pending_se_anula_y_se_persiste() {
            SupplierInvoice pendiente = SupplierInvoiceMother.nueva(CO, BR, SUP, "FAC-001",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                    BigDecimal.ZERO, BigDecimal.ZERO);
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(pendiente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SupplierInvoiceDto dto = service.execute(INVOICE_ID, COMPANY_ID, ACTOR_ID);

            assertThat(dto.status()).isEqualTo(SupplierInvoiceStatus.CANCELLED);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SupplierInvoiceStatus.CANCELLED);
            assertThat(captor.getValue().getUpdatedBy()).isEqualTo(ACTOR_ID);
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

            assertThatThrownBy(() -> service.execute(INVOICE_ID, COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(SupplierInvoiceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(INVOICE_ID));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una factura con abonos no se puede anular")
        void una_factura_con_abonos_no_se_puede_anular() {
            SupplierInvoice conAbono = SupplierInvoiceMother.conEstado(CO, BR, SUP, "FAC-001",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                    BigDecimal.ZERO, BigDecimal.ZERO, SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("100000"),
                            LocalDate.of(2026, 1, 20), SupplierInvoicePaymentMethod.CASH, null)));
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(conAbono));

            assertThatThrownBy(() -> service.execute(INVOICE_ID, COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(InvalidSupplierInvoiceStateException.class)
                    .hasMessageContaining("Only a PENDING invoice");

            verify(repository, never()).save(any());
        }
    }
}
