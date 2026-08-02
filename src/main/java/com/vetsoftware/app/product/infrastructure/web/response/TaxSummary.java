package com.vetsoftware.app.product.infrastructure.web.response;

import java.math.BigDecimal;

public record TaxSummary(Long id, String name, BigDecimal percentage) {
}
