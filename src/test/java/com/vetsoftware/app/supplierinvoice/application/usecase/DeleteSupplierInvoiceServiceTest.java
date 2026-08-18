package com.vetsoftware.app.supplierinvoice.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import com.vetsoftware.app.supplierinvoice.testsupport.SupplierInvoiceMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSupplierInvoiceService")
class DeleteSupplierInvoiceServiceTest {

    private static final Long INVOICE_ID = 55L;
    private static final Long COMPANY_ID = 1L;
    private static final CompanyRef CO = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    private static final BranchRef BR = new BranchRef(3L, "Sede Centro");
    private static final SupplierRef SUP = new SupplierRef(7L, "Distribuidora Sur", "800111222");

    @Mock
    private SupplierInvoiceRepository repository;

    @InjectMocks
    private DeleteSupplierInvoiceService service;

    @Nested
    @DisplayName("baja")
    class Baja {

        @Test
        @DisplayName("una factura existente se da de baja")
        void una_factura_existente_se_da_de_baja() {
            SupplierInvoice factura = SupplierInvoiceMother.nueva(CO, BR, SUP, "FAC-001",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                    BigDecimal.ZERO, BigDecimal.ZERO);
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));

            service.execute(INVOICE_ID, COMPANY_ID);

            verify(repository).delete(INVOICE_ID, COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("factura inexistente en la empresa: no se borra nada")
        void factura_inexistente_no_se_borra_nada() {
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(INVOICE_ID, COMPANY_ID))
                    .isInstanceOf(SupplierInvoiceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(INVOICE_ID));

            verify(repository, never()).delete(any(), any());
        }
    }
}
