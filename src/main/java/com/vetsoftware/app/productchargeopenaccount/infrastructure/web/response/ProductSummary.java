package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response;

import java.math.BigDecimal;

public record ProductSummary(Long id, String name, String code, BigDecimal salePrice) {}
