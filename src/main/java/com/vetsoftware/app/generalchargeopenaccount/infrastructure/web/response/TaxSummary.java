package com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.response;

import java.math.BigDecimal;

public record TaxSummary(Long id, String name, BigDecimal percentage) {}
