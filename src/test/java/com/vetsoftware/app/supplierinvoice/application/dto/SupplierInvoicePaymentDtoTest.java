package com.vetsoftware.app.supplierinvoice.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePayment;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import com.vetsoftware.app.supplierinvoice.testsupport.SupplierInvoiceMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SupplierInvoicePaymentDto")
class SupplierInvoicePaymentDtoTest {

    @Test
    @DisplayName("from mapea el abono campo por campo")
    void from_mapea_el_abono_campo_por_campo() {
        SupplierInvoicePayment abono = SupplierInvoiceMother.abono(new BigDecimal("165000"),
                LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER, "TRF-9");

        SupplierInvoicePaymentDto dto = SupplierInvoicePaymentDto.from(abono);

        assertThat(dto.id()).isNull();
        assertThat(dto.amount()).isEqualByComparingTo("165000");
        assertThat(dto.paymentDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(dto.method()).isEqualTo(SupplierInvoicePaymentMethod.TRANSFER);
        assertThat(dto.reference()).isEqualTo("TRF-9");
        assertThat(dto.note()).isNull();
        assertThat(dto.createdDate()).isEqualTo(SupplierInvoiceMother.CREADA);
        assertThat(dto.createdBy()).isEqualTo(SupplierInvoiceMother.AUTOR);
    }
}
