package com.vetsoftware.app.taxreturn.testsupport;

import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import com.vetsoftware.app.taxreturn.domain.VatFrequency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures de {@link TaxReturn}.
 *
 * <p>
 * <b>La entidad se construye de verdad, nunca se mockea.</b> Un
 * {@code TaxReturn} mockeado no valida ninguna de sus diez comprobaciones, asi
 * que el test pasaria con combinaciones que produccion rechaza.
 *
 * <p>
 * <b>El constructor publico se expone a proposito.</b> No es solo el que usan
 * las factorias: es por donde {@code TaxReturnJpaMapper.toDomain} reconstruye
 * una fila leida de la base, con su {@code id} ya puesto. Varias invariantes
 * —la autocorreccion entre ellas— solo son alcanzables por ese camino, y el
 * unico modo de ejercitarlas es el mismo que usa el mapper.
 */
public final class TaxReturnMother {

    public static final int ANIO = 2026;
    public static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 4, 10, 9, 0);
    public static final String MUNICIPIO_ICA = "05001";

    private TaxReturnMother() {
    }

    /** Retencion en la fuente de marzo, en borrador. El caso mas corriente. */
    public static TaxReturn borradorDeRetencion() {
        return TaxReturn.draft(TaxKind.WITHHOLDING, ANIO, ANIO + "-M03", null, null,
                new BigDecimal("4200000.00"), BigDecimal.ZERO, new BigDecimal("4200000.00"),
                BigDecimal.ZERO, CREADA_EL);
    }

    /** IVA bimestral del primer bimestre, en borrador. */
    public static TaxReturn borradorDeIva() {
        return TaxReturn.draft(TaxKind.VAT, ANIO, ANIO + "-B01", null, VatFrequency.BIMONTHLY,
                new BigDecimal("9500000.00"), new BigDecimal("3100000.00"),
                new BigDecimal("6400000.00"), BigDecimal.ZERO, CREADA_EL);
    }

    /** ICA de Medellin, primer bimestre, en borrador. */
    public static TaxReturn borradorDeIca() {
        return TaxReturn.draft(TaxKind.ICA, ANIO, ANIO + "-B01", MUNICIPIO_ICA, null,
                new BigDecimal("1800000.00"), BigDecimal.ZERO, new BigDecimal("1800000.00"),
                BigDecimal.ZERO, CREADA_EL);
    }

    /** Renta anual, en borrador. */
    public static TaxReturn borradorDeRenta() {
        return TaxReturn.draft(TaxKind.INCOME_TAX, ANIO, ANIO + "-A", null, null,
                new BigDecimal("52000000.00"), new BigDecimal("41000000.00"),
                new BigDecimal("11000000.00"), BigDecimal.ZERO, CREADA_EL);
    }

    /** La misma retencion, ya presentada y con id, tal como volveria de la base. */
    public static TaxReturn retencionPresentada(Long id) {
        return conId(id, borradorDeRetencion()).file(LocalDateTime.of(2026, 4, 12, 10, 30), 990L,
                "REC-2026-000123", "s3://declaraciones/2026/M03.pdf", LocalDate.of(2029, 4, 12));
    }

    /**
     * Reconstruye la declaracion con un {@code id}, imitando lo que hace el mapper
     * al leerla de la base: las factorias nacen sin id y hay invariantes que solo
     * se pueden ejercitar con uno puesto.
     */
    public static TaxReturn conId(Long id, TaxReturn origen) {
        return new TaxReturn(id, origen.getTaxKind(), origen.getFiscalYear(),
                origen.getFiscalPeriodKey(), origen.getSequenceNumber(),
                origen.getMunicipalityCode(), origen.getVatFrequency(), origen.getStatus(),
                origen.getFiledAt(), origen.getFiledBySystemUserId(), origen.getReceiptRef(),
                origen.getFileRef(), origen.getTotalGenerated(), origen.getTotalDeductible(),
                origen.getBalancePayable(), origen.getBalanceCredit(), origen.getFirmezaUntil(),
                origen.getCorrectsReturnId(), origen.getCreatedDate(), origen.getVersion());
    }

    /**
     * El constructor crudo, para los casos que necesitan una combinacion que
     * ninguna factoria produce. Los valores por defecto son los de una retencion
     * valida; el caso cambia solo lo que quiere romper.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static TaxReturn crudo(Long id, TaxKind taxKind, int fiscalYear, String fiscalPeriodKey,
            int sequenceNumber, String municipalityCode, VatFrequency vatFrequency,
            TaxReturnStatus status, Long correctsReturnId) {
        return new TaxReturn(id, taxKind, fiscalYear, fiscalPeriodKey, sequenceNumber,
                municipalityCode, vatFrequency, status, null, null, null, null,
                new BigDecimal("4200000.00"), BigDecimal.ZERO, new BigDecimal("4200000.00"),
                BigDecimal.ZERO, null, correctsReturnId, CREADA_EL, null);
    }
}
