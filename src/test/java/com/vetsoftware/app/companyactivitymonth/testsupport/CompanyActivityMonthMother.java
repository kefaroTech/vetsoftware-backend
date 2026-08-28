package com.vetsoftware.app.companyactivitymonth.testsupport;

import com.vetsoftware.app.companyactivitymonth.application.command.RecordCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.domain.ActivityPeriodKey;
import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fixtures del modulo companyactivitymonth. */
public final class CompanyActivityMonthMother {

    public static final Long MONTH_ID = 900L;
    public static final Long COMPANY_ID = 9L;

    public static final ActivityPeriodKey MARZO_2026 = new ActivityPeriodKey("2026-03");

    /** 2026 no es bisiesto: febrero tiene 28 dias. */
    public static final ActivityPeriodKey FEBRERO_2026 = new ActivityPeriodKey("2026-02");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 1, 0, 5);

    private CompanyActivityMonthMother() {
    }

    /** Fila pagada, con actividad plena. El caso por defecto. */
    public static CompanyActivityMonth pagada() {
        return new CompanyActivityMonth(MONTH_ID, COMPANY_ID, MARZO_2026, CommercialState.PAID, 20,
                5, 340, new BigDecimal("199990.00"), CREADO, 0L);
    }

    /** Fila dormida: cero dias activos ese mes. */
    public static CompanyActivityMonth dormida() {
        return new CompanyActivityMonth(MONTH_ID, COMPANY_ID, MARZO_2026, CommercialState.FREE, 0,
                0, 0, BigDecimal.ZERO, CREADO, 0L);
    }

    public static RecordCompanyActivityMonthCommand comandoRegistrar() {
        return new RecordCompanyActivityMonthCommand(COMPANY_ID, MARZO_2026.value(),
                CommercialState.PAID, 20, 5, 340, new BigDecimal("199990.00"));
    }
}
