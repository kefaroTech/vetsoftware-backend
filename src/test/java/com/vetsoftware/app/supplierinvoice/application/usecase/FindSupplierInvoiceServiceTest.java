package com.vetsoftware.app.supplierinvoice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
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
@DisplayName("FindSupplierInvoiceService")
class FindSupplierInvoiceServiceTest {

    private static final Long INVOICE_ID = 55L;
    private static final Long COMPANY_ID = 1L;
    private static final CompanyRef CO = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    private static final BranchRef BR = new BranchRef(3L, "Sede Centro");
    private static final SupplierRef SUP = new SupplierRef(7L, "Distribuidora Sur", "800111222");

    @Mock
    private SupplierInvoiceRepository repository;

    @InjectMocks
    private FindSupplierInvoiceService service;

    @Nested
    @DisplayName("consulta")
    class Consulta {

        @Test
        @DisplayName("devuelve la factura mapeada a DTO")
        void devuelve_la_factura_mapeada_a_dto() {
            SupplierInvoice factura = SupplierInvoiceMother.nueva(CO, BR, SUP, "FAC-001",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                    BigDecimal.ZERO, BigDecimal.ZERO);
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));

            SupplierInvoiceDto dto = service.findById(INVOICE_ID, COMPANY_ID);

            assertThat(dto.invoiceNumber()).isEqualTo("FAC-001");
            assertThat(dto.company().id()).isEqualTo(COMPANY_ID);
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

            assertThatThrownBy(() -> service.findById(INVOICE_ID, COMPANY_ID))
                    .isInstanceOf(SupplierInvoiceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(INVOICE_ID));
        }
    }
}
