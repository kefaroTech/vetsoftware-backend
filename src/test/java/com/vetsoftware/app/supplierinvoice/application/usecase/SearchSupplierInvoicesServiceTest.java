package com.vetsoftware.app.supplierinvoice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplierinvoice.application.command.SearchSupplierInvoicesCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import com.vetsoftware.app.supplierinvoice.testsupport.SupplierInvoiceMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchSupplierInvoicesService")
class SearchSupplierInvoicesServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final CompanyRef CO = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    private static final BranchRef BR = new BranchRef(3L, "Sede Centro");
    private static final SupplierRef SUP = new SupplierRef(7L, "Distribuidora Sur", "800111222");

    @Mock
    private SupplierInvoiceRepository repository;

    @InjectMocks
    private SearchSupplierInvoicesService service;

    private SearchSupplierInvoicesCommand comando() {
        return new SearchSupplierInvoicesCommand(COMPANY_ID, null, null, null, null, null, 0, 20);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea el contenido paginado a DTOs conservando los metadatos")
        void mapea_el_contenido_paginado_conservando_metadatos() {
            SupplierInvoice factura1 = SupplierInvoiceMother.nueva(CO, BR, SUP, "FAC-001",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                    BigDecimal.ZERO, BigDecimal.ZERO);
            SupplierInvoice factura2 = SupplierInvoiceMother.nueva(CO, BR, SUP, "FAC-002",
                    LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 14), new BigDecimal("200000"),
                    BigDecimal.ZERO, BigDecimal.ZERO);
            when(repository.search(comando()))
                    .thenReturn(new PageResult<>(List.of(factura1, factura2), 0, 20, 2, 1));

            PageResult<SupplierInvoiceDto> pagina = service.execute(comando());

            assertThat(pagina.content()).hasSize(2);
            assertThat(pagina.content().get(0).invoiceNumber()).isEqualTo("FAC-001");
            assertThat(pagina.content().get(1).invoiceNumber()).isEqualTo("FAC-002");
            assertThat(pagina.totalElements()).isEqualTo(2);
            assertThat(pagina.page()).isZero();
        }

        @Test
        @DisplayName("sin resultados devuelve una pagina vacia")
        void sin_resultados_devuelve_una_pagina_vacia() {
            when(repository.search(comando())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

            PageResult<SupplierInvoiceDto> pagina = service.execute(comando());

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isZero();
        }
    }
}
