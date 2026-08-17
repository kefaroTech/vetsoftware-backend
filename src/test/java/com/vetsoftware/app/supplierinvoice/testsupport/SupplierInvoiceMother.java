package com.vetsoftware.app.supplierinvoice.testsupport;

import com.vetsoftware.app.supplierinvoice.application.dto.AccountsPayableAgingDto;
import com.vetsoftware.app.supplierinvoice.application.dto.BranchSummaryDto;
import com.vetsoftware.app.supplierinvoice.application.dto.CompanySummaryDto;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoicePaymentDto;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierSummaryDto;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePayment;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Object mother de la feature {@code supplierinvoice}.
 *
 * <p>
 * Todas las marcas de tiempo son fijas a proposito: la factura de proveedor se
 * reparte en tramos de antiguedad por diferencia de fechas, asi que un fixture
 * que llamara a {@code now()} daria un test que cambia de tramo al cruzar
 * medianoche.
 */
public final class SupplierInvoiceMother {

    /**
     * Empleado que firma los fixtures (coincide con el sembrado por SchemaSeed).
     */
    public static final Long AUTOR = 940L;

    /** Momento de creacion fijo de todos los agregados de prueba. */
    public static final LocalDateTime CREADA = LocalDateTime.of(2026, 1, 15, 10, 30);

    private SupplierInvoiceMother() {
    }

    /** Factura recien registrada: sin id, PENDING y sin abonos. */
    public static SupplierInvoice nueva(CompanyRef empresa, BranchRef sede, SupplierRef proveedor,
            String numero, LocalDate emision, LocalDate vencimiento, BigDecimal subtotal,
            BigDecimal impuesto, BigDecimal retencion) {
        return conEstado(empresa, sede, proveedor, numero, emision, vencimiento, subtotal, impuesto,
                retencion, SupplierInvoiceStatus.PENDING, List.of());
    }

    /** Factura en el estado indicado, con los abonos ya aplicados. */
    public static SupplierInvoice conEstado(CompanyRef empresa, BranchRef sede,
            SupplierRef proveedor, String numero, LocalDate emision, LocalDate vencimiento,
            BigDecimal subtotal, BigDecimal impuesto, BigDecimal retencion,
            SupplierInvoiceStatus estado, List<SupplierInvoicePayment> abonos) {
        return new SupplierInvoice(null, empresa, sede, proveedor, null, null, numero, emision,
                vencimiento, subtotal, impuesto, retencion, estado, "Compra de insumos", abonos,
                CREADA, AUTOR, null, null, null, true);
    }

    /** Abono todavia sin persistir (id nulo). */
    public static SupplierInvoicePayment abono(BigDecimal monto, LocalDate fecha,
            SupplierInvoicePaymentMethod metodo, String referencia) {
        return new SupplierInvoicePayment(null, monto, fecha, metodo, referencia, null, CREADA,
                AUTOR);
    }

    /**
     * DTO de salida de una factura abonada parcialmente, para las rodajas web. El
     * dinero cuadra: total 1.190.000, retencion 25.000, abonado 165.000, saldo
     * 1.000.000.
     */
    public static SupplierInvoiceDto dtoParcial(Long companyId) {
        return new SupplierInvoiceDto(55L,
                new CompanySummaryDto(companyId, "Clinica Norte", "NIT-900"),
                new BranchSummaryDto(3L, "Sede Centro"),
                new SupplierSummaryDto(7L, "Distribuidora Sur", "800111222"), 31L, 41L, "FAC-001",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000.00"),
                new BigDecimal("190000.00"), new BigDecimal("25000.00"),
                new BigDecimal("1190000.00"), new BigDecimal("1165000.00"),
                new BigDecimal("165000.00"), new BigDecimal("1000000.00"),
                SupplierInvoiceStatus.PARTIAL, "Compra de insumos",
                List.of(new SupplierInvoicePaymentDto(88L, new BigDecimal("165000.00"),
                        LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER, "TRF-9",
                        null, CREADA, AUTOR)),
                CREADA, AUTOR, null, null, 1L, true);
    }

    /** DTO de salida de una factura anulada: sin abonos y con saldo cero. */
    public static SupplierInvoiceDto dtoAnulada(Long companyId) {
        return new SupplierInvoiceDto(55L,
                new CompanySummaryDto(companyId, "Clinica Norte", "NIT-900"),
                new BranchSummaryDto(3L, "Sede Centro"),
                new SupplierSummaryDto(7L, "Distribuidora Sur", "800111222"), null, null, "FAC-001",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000.00"),
                new BigDecimal("190000.00"), new BigDecimal("25000.00"),
                new BigDecimal("1190000.00"), new BigDecimal("1165000.00"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), SupplierInvoiceStatus.CANCELLED, "Compra de insumos",
                List.of(), CREADA, AUTOR, CREADA, AUTOR, 2L, true);
    }

    /** Reporte de antiguedad con un proveedor repartido en dos tramos. */
    public static AccountsPayableAgingDto aging(LocalDate asOf) {
        AccountsPayableAgingDto.Bucket bucket = new AccountsPayableAgingDto.Bucket(
                new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("300.00"));
        return new AccountsPayableAgingDto(asOf, List.of(new AccountsPayableAgingDto.SupplierRow(7L,
                "Distribuidora Sur", "800111222", bucket)), bucket);
    }
}
