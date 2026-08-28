package com.vetsoftware.app.externalinvoicereconciliation.testsupport;

import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Escenarios de conciliacion listos para usar.
 *
 * <p>
 * <b>Los numeros no son arbitrarios y no conviene cambiarlos a la ligera.</b>
 * El total propio es {@code 119000.00} = base {@code 100000.00} + impuesto
 * {@code 19000.00}, que es la forma que tiene un documento de cobro real de
 * este proyecto. A partir de ahi, un total externo de {@code 118998.00} deja
 * una diferencia de exactamente {@code +2.00} —el limite de la tolerancia— y
 * uno de {@code 119002.00} la deja en {@code -2.00}: los dos bordes con signo,
 * que es donde se rompen las implementaciones que comparan sin {@code abs} o
 * con {@code equals}.
 *
 * <p>
 * <b>Las cuatro fechas son deliberadamente distintas entre si.</b> Si un mapper
 * cruza {@code createdDate} con {@code resolvedAt}, o
 * {@code resolutionValidUntil} con cualquiera de las dos, la asercion cae; con
 * la misma fecha en todas, no.
 */
public final class ExternalInvoiceReconciliationMother {

    public static final Long EMPRESA = 900L;
    public static final Long OTRA_EMPRESA = 901L;
    public static final Long DOCUMENTO = 8600L;
    public static final Long FIRMANTE = 990L;

    public static final BigDecimal TOTAL_PROPIO = new BigDecimal("119000.00");
    public static final BigDecimal IMPUESTO_PROPIO = new BigDecimal("19000.00");

    public static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 5, 14, 30, 15);
    public static final LocalDateTime RESUELTO_EL = LocalDateTime.of(2026, 4, 11, 9, 20, 45);
    public static final LocalDate VIGENTE_HASTA = LocalDate.of(2027, 1, 31);
    public static final String PERIODO_CONTABLE = "2026-03";

    private ExternalInvoiceReconciliationMother() {
    }

    /** Conciliacion recien abierta: {@code MISSING_EXTERNAL} y sin id. */
    public static ExternalInvoiceReconciliation abierta() {
        return ExternalInvoiceReconciliation.open(EMPRESA, DOCUMENTO, TOTAL_PROPIO, IMPUESTO_PROPIO,
                CREADO_EL);
    }

    /** La misma, ya persistida: con id y con la version que trae la base. */
    public static ExternalInvoiceReconciliation abiertaConId(Long id) {
        return new ExternalInvoiceReconciliation(id, EMPRESA, DOCUMENTO, null, null, null, null,
                null, null, TOTAL_PROPIO, IMPUESTO_PROPIO, null, null, null,
                ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL, null, null, null, null,
                CREADO_EL, 0L);
    }

    /** Abierta contra otro documento de otra empresa. */
    public static ExternalInvoiceReconciliation abiertaDe(Long companyId, Long billingDocumentId) {
        return ExternalInvoiceReconciliation.open(companyId, billingDocumentId, TOTAL_PROPIO,
                IMPUESTO_PROPIO, CREADO_EL);
    }

    /**
     * Ya conciliada contra la factura del tercero. El estado lo decide el dominio a
     * partir del total externo que se le pase: por eso este metodo NO lo recibe.
     */
    public static ExternalInvoiceReconciliation conFacturaExterna(Long id,
            BigDecimal totalExterno) {
        ExternalInvoiceReconciliation reconciliation = abiertaConId(id);
        reconciliation.match("FE-1043", "CUFE-0011", totalExterno, new BigDecimal("19000.00"), null,
                null, null, null);
        return reconciliation;
    }

    /** Conciliada y ademas cerrada con firma, nota y periodo contable. */
    public static ExternalInvoiceReconciliation resuelta(Long id, BigDecimal totalExterno) {
        ExternalInvoiceReconciliation reconciliation = conFacturaExterna(id, totalExterno);
        reconciliation.resolve(FIRMANTE, "Ajuste por redondeo del impuesto", PERIODO_CONTABLE,
                RESUELTO_EL);
        return reconciliation;
    }
}
