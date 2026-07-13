package com.vetsoftware.app.inventory.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/** Hoja de conteo físico: sede + líneas contadas por producto. {@code branchId} lo acota el controller al alcance. */
public record RecordCountRequest(
        @NotNull Long branchId,
        String note,
        @NotEmpty @Valid List<Line> lines
) {
    public record Line(@NotNull Long productId, @PositiveOrZero int countedQuantity) {}
}
