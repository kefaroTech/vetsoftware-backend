package com.vetsoftware.app.vatfilingperiod.domain;

/**
 * No hay periodicidad de IVA publicada para lo que se pidio. Mapea a 404.
 *
 * <p>
 * Fallar es lo correcto: suponer bimestral porque «suele serlo» produciria
 * declaraciones en meses que no tocan, con sus sanciones por extemporaneidad.
 */
public class VatFilingPeriodNotFoundException extends RuntimeException {

    public VatFilingPeriodNotFoundException(Long id) {
        super("VAT filing period not found: " + id);
    }

    public VatFilingPeriodNotFoundException(int fiscalYear) {
        super("VAT filing period not published for fiscal year: " + fiscalYear);
    }
}
