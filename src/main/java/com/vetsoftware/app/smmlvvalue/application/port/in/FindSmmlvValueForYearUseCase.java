package com.vetsoftware.app.smmlvvalue.application.port.in;

import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Resuelve el salario minimo <strong>del ano que se pide</strong>, con su
 * estado. Quien lo consume recibe siempre la pareja cifra + estado: no hay
 * forma de leer el numero sin enterarse de que esta suspendido.
 */
public interface FindSmmlvValueForYearUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('smmlv.read') and @authz.isMyCompany(#companyId))")
    SmmlvValueDto findByYear(int fiscalYear, Long companyId);
}
