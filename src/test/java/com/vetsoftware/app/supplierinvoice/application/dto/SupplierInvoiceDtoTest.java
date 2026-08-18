package com.vetsoftware.app.supplierinvoice.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import com.vetsoftware.app.supplierinvoice.testsupport.SupplierInvoiceMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SupplierInvoiceDto")
class SupplierInvoiceDtoTest {

    private static final CompanyRef CO = new CompanyRef(1L, "Clinica Norte", "NIT-900");
    private static final BranchRef BR = new BranchRef(3L, "Sede Centro");
    private static final SupplierRef SUP = new SupplierRef(7L, "Distribuidora Sur", "800111222");

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("mapea cabecera, totales derivados y abonos campo por campo")
        void mapea_cabecera_totales_y_abonos() {
            SupplierInvoice inv = SupplierInvoiceMother.conEstado(CO, BR, SUP, "FAC-001",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                    new BigDecimal("190000"), new BigDecimal("25000"),
                    SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("165000"),
                            LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER,
                            "TRF-9")));

            SupplierInvoiceDto dto = SupplierInvoiceDto.from(inv);

            assertThat(dto.company())
                    .isEqualTo(new CompanySummaryDto(1L, "Clinica Norte", "NIT-900"));
            assertThat(dto.branch()).isEqualTo(new BranchSummaryDto(3L, "Sede Centro"));
            assertThat(dto.supplier())
                    .isEqualTo(new SupplierSummaryDto(7L, "Distribuidora Sur", "800111222"));
            assertThat(dto.invoiceNumber()).isEqualTo("FAC-001");
            assertThat(dto.issueDate()).isEqualTo(LocalDate.of(2026, 1, 10));
            assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 2, 9));
            assertThat(dto.subtotal()).isEqualByComparingTo("1000000");
            assertThat(dto.taxAmount()).isEqualByComparingTo("190000");
            assertThat(dto.withholdingAmount()).isEqualByComparingTo("25000");
            assertThat(dto.total()).isEqualByComparingTo("1190000");
            assertThat(dto.payableAmount()).isEqualByComparingTo("1165000");
            assertThat(dto.paidAmount()).isEqualByComparingTo("165000");
            assertThat(dto.balance()).isEqualByComparingTo("1000000");
            assertThat(dto.status()).isEqualTo(SupplierInvoiceStatus.PARTIAL);
            assertThat(dto.notes()).isEqualTo("Compra de insumos");
            assertThat(dto.payments()).hasSize(1);
            assertThat(dto.payments().get(0).amount()).isEqualByComparingTo("165000");
            assertThat(dto.payments().get(0).method())
                    .isEqualTo(SupplierInvoicePaymentMethod.TRANSFER);
            assertThat(dto.createdDate()).isEqualTo(SupplierInvoiceMother.CREADA);
            assertThat(dto.createdBy()).isEqualTo(SupplierInvoiceMother.AUTOR);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("una factura recien creada no trae abonos ni referencias de compra")
        void una_factura_recien_creada_no_trae_abonos() {
            SupplierInvoice inv = SupplierInvoiceMother.nueva(CO, BR, SUP, "FAC-002",
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), new BigDecimal("500000"),
                    BigDecimal.ZERO, BigDecimal.ZERO);

            SupplierInvoiceDto dto = SupplierInvoiceDto.from(inv);

            assertThat(dto.payments()).isEmpty();
            assertThat(dto.purchaseOrderId()).isNull();
            assertThat(dto.goodsReceiptId()).isNull();
            assertThat(dto.status()).isEqualTo(SupplierInvoiceStatus.PENDING);
            assertThat(dto.paidAmount()).isEqualByComparingTo("0");
        }
    }
}
