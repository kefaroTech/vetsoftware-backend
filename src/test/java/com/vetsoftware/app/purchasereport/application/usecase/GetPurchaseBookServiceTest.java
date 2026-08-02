package com.vetsoftware.app.purchasereport.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import com.vetsoftware.app.purchasereport.application.port.out.PurchaseDocumentQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Test del libro de compras: agrega las facturas del periodo y calcula los
 * totales.
 */
class GetPurchaseBookServiceTest {

    private static PurchaseDocumentQueryPort.PurchaseDocumentView view(long id, String base,
            String tax, String with, String total, String paid, String balance) {
        return new PurchaseDocumentQueryPort.PurchaseDocumentView(id, "Distribuidora",
                "800111222-3", "FV-" + id, LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 5),
                new BigDecimal(base), new BigDecimal(tax), new BigDecimal(with),
                new BigDecimal(total), new BigDecimal(paid), new BigDecimal(balance), "PARTIAL");
    }

    @Test
    void agrega_entradas_y_totaliza_el_periodo() {
        PurchaseDocumentQueryPort port = (companyId, from, to, branchId) -> List.of(
                view(1, "1000", "190", "0", "1190", "1190", "0"),
                view(2, "500", "95", "5", "595", "0", "590"));
        var service = new GetPurchaseBookService(port);

        PurchaseBookDto book = service.get(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null);

        assertThat(book.entries()).hasSize(2);
        PurchaseBookDto.TotalsDto t = book.totals();
        assertThat(t.invoiceCount()).isEqualTo(2);
        assertThat(t.subtotal()).isEqualByComparingTo("1500");
        assertThat(t.taxAmount()).isEqualByComparingTo("285");
        assertThat(t.withholdingAmount()).isEqualByComparingTo("5");
        assertThat(t.total()).isEqualByComparingTo("1785");
        assertThat(t.paidAmount()).isEqualByComparingTo("1190");
        assertThat(t.balance()).isEqualByComparingTo("590");
    }

    @Test
    void periodo_sin_compras_da_totales_en_cero() {
        PurchaseDocumentQueryPort port = (companyId, from, to, branchId) -> List.of();
        var service = new GetPurchaseBookService(port);
        PurchaseBookDto book = service.get(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null);
        assertThat(book.entries()).isEmpty();
        assertThat(book.totals().total()).isEqualByComparingTo("0");
        assertThat(book.totals().invoiceCount()).isZero();
    }
}
