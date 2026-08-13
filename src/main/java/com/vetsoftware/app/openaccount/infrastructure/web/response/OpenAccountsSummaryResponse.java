package com.vetsoftware.app.openaccount.infrastructure.web.response;

import java.math.BigDecimal;

public record OpenAccountsSummaryResponse(long openCount, long closedCount,
        BigDecimal totalOutstanding) {
}
