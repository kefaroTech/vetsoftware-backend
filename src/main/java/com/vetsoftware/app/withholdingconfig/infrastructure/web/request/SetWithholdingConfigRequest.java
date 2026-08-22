package com.vetsoftware.app.withholdingconfig.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record SetWithholdingConfigRequest(
        @NotNull(message = "La tarifa de retención en la fuente es obligatoria.") @PositiveOrZero(message = "La tarifa de retención en la fuente no puede ser negativa.") BigDecimal reteFuenteRate,
        @NotNull(message = "La tarifa de retención de IVA es obligatoria.") @PositiveOrZero(message = "La tarifa de retención de IVA no puede ser negativa.") BigDecimal reteIvaRate,
        @NotNull(message = "La tarifa de retención de ICA es obligatoria.") @PositiveOrZero(message = "La tarifa de retención de ICA no puede ser negativa.") BigDecimal reteIcaRate) {
}
