package com.vetsoftware.app.limitdimension.application.command;

import com.vetsoftware.app.limitdimension.domain.MeasureKind;
import java.time.LocalDate;

/**
 * Declarar un eje limitable nuevo.
 *
 * <p>
 * No lleva {@code companyId} y no puede llevarlo: el catálogo de ejes es global
 * de plataforma. Por eso su puerto va cerrado a {@code hasRole('SYSTEM')} a
 * secas y no a {@code @authz.isMyCompany(...)}, que aquí no tendría nada que
 * comparar.
 */
public record CreateLimitDimensionCommand(String code, String name, MeasureKind measureKind,
        Long subModuleId, Integer releaseDelayDays, LocalDate availableFrom) {
}
