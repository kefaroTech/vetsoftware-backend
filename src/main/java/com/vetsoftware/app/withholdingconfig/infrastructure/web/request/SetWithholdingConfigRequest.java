package com.vetsoftware.app.withholdingconfig.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record SetWithholdingConfigRequest(@NotNull @PositiveOrZero BigDecimal reteFuenteRate,
        @NotNull @PositiveOrZero BigDecimal reteIvaRate,
        @NotNull @PositiveOrZero BigDecimal reteIcaRate) {
}
