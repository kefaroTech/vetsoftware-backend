package com.vetsoftware.app.withholdingconfig.testsupport;

import com.vetsoftware.app.withholdingconfig.domain.CompanyRef;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class WithholdingConfigMother {

    public static final Long COMPANY_ID = 5L;
    public static final CompanyRef VETERINARIA_CENTRAL = new CompanyRef(COMPANY_ID,
            "Veterinaria Central", "900123456-1");
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 8, 30);

    private WithholdingConfigMother() {
    }

    public static WithholdingConfig configValida() {
        return new WithholdingConfig(10L, VETERINARIA_CENTRAL, new BigDecimal("2.5"),
                new BigDecimal("15.0"), new BigDecimal("1.0"), CREADO, null, true);
    }

    public static WithholdingConfig deshabilitada() {
        return new WithholdingConfig(11L, VETERINARIA_CENTRAL, new BigDecimal("2.5"),
                new BigDecimal("15.0"), new BigDecimal("1.0"), CREADO, null, false);
    }
}
