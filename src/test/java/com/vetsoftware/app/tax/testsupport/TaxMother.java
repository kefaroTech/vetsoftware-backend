package com.vetsoftware.app.tax.testsupport;

import com.vetsoftware.app.tax.application.command.CreateTaxCommand;
import com.vetsoftware.app.tax.application.command.UpdateTaxCommand;
import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.domain.TaxScheme;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fixtures del modulo tax. */
public final class TaxMother {

    public static final Long TAX_ID = 500L;
    public static final Long COMPANY_ID = 9L;
    public static final Long EMPLOYEE_ID = 4L;

    public static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "900123456");
    public static final CompanyRef OTRA_EMPRESA = new CompanyRef(10L, "Clinica Sur", "900654321");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 12, 9, 15);
    public static final LocalDateTime ACTUALIZADO = LocalDateTime.of(2026, 3, 13, 8, 0);

    private TaxMother() {
    }

    /** Impuesto activo, version 0, sin actualizar. El caso por defecto. */
    public static Tax activo() {
        return new Tax(TAX_ID, "IVA General", new BigDecimal("19.00"), TaxScheme.IVA, EMPRESA,
                CREADO, null, null, 0L, true);
    }

    /** Impuesto ya actualizado y pausado. */
    public static Tax inactivo() {
        return new Tax(TAX_ID, "IVA General", new BigDecimal("19.00"), TaxScheme.IVA, EMPRESA,
                CREADO, ACTUALIZADO, EMPLOYEE_ID, 1L, false);
    }

    public static CreateTaxCommand comandoCrear() {
        return new CreateTaxCommand("IVA General", new BigDecimal("19.00"), TaxScheme.IVA,
                COMPANY_ID);
    }

    public static UpdateTaxCommand comandoActualizar() {
        return new UpdateTaxCommand(TAX_ID, "IVA Reducido", new BigDecimal("5.00"), TaxScheme.INC,
                COMPANY_ID, EMPLOYEE_ID, 0L);
    }
}
