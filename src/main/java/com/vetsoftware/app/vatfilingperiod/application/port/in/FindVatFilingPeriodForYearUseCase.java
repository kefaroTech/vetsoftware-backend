package com.vetsoftware.app.vatfilingperiod.application.port.in;

import com.vetsoftware.app.vatfilingperiod.application.dto.VatFilingPeriodDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Resuelve la periodicidad <strong>del ano que se pide</strong>. Recibe la
 * empresa aunque la tabla no la tenga: leen los dos lados y es lo que permite
 * cerrar la via del empleado a su propia empresa.
 */
public interface FindVatFilingPeriodForYearUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('vatfiling.read') and @authz.isMyCompany(#companyId))")
    VatFilingPeriodDto findByYear(int fiscalYear, Long companyId);
}
