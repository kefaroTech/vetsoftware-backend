package com.vetsoftware.app.salesreport.testsupport;

import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.PaymentLineView;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.SalesDocumentView;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.TaxLineView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fixtures del modulo salesreport. Construye las vistas de solo lectura del
 * {@code SalesDocumentQueryPort} que consumen los use cases del libro de ventas
 * y de la conciliacion — son records, se instancian de verdad, nunca se
 * mockean.
 */
public final class SalesDocumentMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long BRANCH_ID = 31L;
    public static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    public static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    private SalesDocumentMother() {
    }

    public static TaxLineView ivaLinea(BigDecimal base, BigDecimal impuesto) {
        return new TaxLineView("IVA", new BigDecimal("19.00"), base, impuesto);
    }

    public static TaxLineView incLinea(BigDecimal base, BigDecimal impuesto) {
        return new TaxLineView("INC", new BigDecimal("8.00"), base, impuesto);
    }

    public static PaymentLineView pagoEfectivo(BigDecimal monto) {
        return new PaymentLineView("EFECTIVO", "10", monto);
    }

    public static PaymentLineView pagoTarjeta(BigDecimal monto) {
        return new PaymentLineView("TARJETA_DEBITO", "48", monto);
    }

    /**
     * Documento completo, pensado para las pruebas de agregacion del libro de
     * ventas.
     */
    public static SalesDocumentView documento(Long id, LocalDate issueDate, String dianStatus,
            BigDecimal base, BigDecimal taxInclusive, List<TaxLineView> taxLines,
            List<PaymentLineView> paymentLines, BigDecimal reteFuente, BigDecimal reteIva,
            BigDecimal reteIca) {
        return new SalesDocumentView(id, "FE_VENTA", "SETP", 990L + id, issueDate, "900123456",
                "Clinica Norte", dianStatus, "cufe-" + id, "cude-" + id, base, taxInclusive,
                taxInclusive, reteFuente, reteIva, reteIca, taxLines, paymentLines);
    }

    /**
     * Documento minimo, pensado para las pruebas de conteo/estado de la
     * conciliacion.
     */
    public static SalesDocumentView documentoConEstado(Long id, LocalDate issueDate,
            String dianStatus) {
        return documento(id, issueDate, dianStatus, BigDecimal.ZERO, BigDecimal.ZERO, List.of(),
                List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
