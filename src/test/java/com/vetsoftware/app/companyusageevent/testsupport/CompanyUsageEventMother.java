package com.vetsoftware.app.companyusageevent.testsupport;

import com.vetsoftware.app.companyusageevent.application.command.AttachUsageEventToChargeCommand;
import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.domain.LimitDimensionRef;
import com.vetsoftware.app.companyusageevent.domain.UsageBranch;
import com.vetsoftware.app.companyusageevent.domain.UsagePeriodKey;
import java.time.LocalDateTime;

/** Fixtures del modulo companyusageevent. */
public final class CompanyUsageEventMother {

    public static final Long EVENT_ID = 800L;
    public static final Long COMPANY_ID = 9L;
    public static final Long DIMENSION_ID = 5L;
    public static final Long ANIMAL_ID = 100L;
    public static final Long CHARGE_ID = 300L;

    public static final LimitDimensionRef DIMENSION_ANIMAL = new LimitDimensionRef(DIMENSION_ID,
            "ANIMAL");

    public static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 3, 10, 9, 14);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 10, 23, 0);
    public static final UsagePeriodKey PERIOD_KEY = UsagePeriodKey.of("2026-03");

    private CompanyUsageEventMother() {
    }

    /** Hecho de uso persistido, facturable, aun sin cargo. El caso por defecto. */
    public static CompanyUsageEvent hechoSinCargo() {
        return new CompanyUsageEvent(EVENT_ID, COMPANY_ID, DIMENSION_ID, UsageBranch.ANIMAL,
                ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, null, CREADO, 0L);
    }

    /** Hecho de uso ya colgado de un cargo. */
    public static CompanyUsageEvent hechoConCargo() {
        return new CompanyUsageEvent(EVENT_ID, COMPANY_ID, DIMENSION_ID, UsageBranch.ANIMAL,
                ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, CHARGE_ID, CREADO, 1L);
    }

    public static RecordCompanyUsageEventCommand comandoRegistrar() {
        return new RecordCompanyUsageEventCommand(COMPANY_ID, "ANIMAL", ANIMAL_ID, OCCURRED_AT,
                PERIOD_KEY.value(), true);
    }

    public static AttachUsageEventToChargeCommand comandoColgarCargo() {
        return new AttachUsageEventToChargeCommand(EVENT_ID, COMPANY_ID, CHARGE_ID);
    }
}
