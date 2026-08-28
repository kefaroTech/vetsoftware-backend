package com.vetsoftware.app.revenuerecognitionline.testsupport;

import com.vetsoftware.app.revenuerecognitionline.domain.RecognitionMethod;
import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fixtures de la feature {@code revenuerecognitionline}. */
public final class RevenueRecognitionLineMother {

    public static final Long LINE_ID = 500L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTHER_COMPANY_ID = 99L;
    public static final Long CHARGE_ID = 42L;
    public static final String PERIOD_KEY = "2026-03";
    public static final String POSTING_PERIOD = "2026-03";
    public static final BigDecimal RECOGNIZED_AMOUNT = new BigDecimal("100.00");
    public static final RecognitionMethod METHOD = RecognitionMethod.STRAIGHT_LINE_DAYS;
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 5, 8, 0);

    private RevenueRecognitionLineMother() {
    }

    /** El renglon original de una clinica. El caso por defecto. */
    public static RevenueRecognitionLine renglon() {
        return renglon(LINE_ID);
    }

    public static RevenueRecognitionLine renglon(Long id) {
        return new RevenueRecognitionLine(id, COMPANY_ID, CHARGE_ID, PERIOD_KEY, POSTING_PERIOD,
                RECOGNIZED_AMOUNT, METHOD, CREADO);
    }

    /**
     * La fila que compensa al renglon por defecto: importe opuesto, un mes despues.
     */
    public static RevenueRecognitionLine compensacion() {
        return new RevenueRecognitionLine(null, COMPANY_ID, CHARGE_ID, PERIOD_KEY, "2026-04",
                RECOGNIZED_AMOUNT.negate(), METHOD, CREADO);
    }
}
