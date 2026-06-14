package com.vetsoftware.app.tax.application.command;

import com.vetsoftware.app.tax.domain.TaxScheme;
import java.math.BigDecimal;

public record CreateTaxCommand(String name, BigDecimal percentage, TaxScheme taxScheme, Long companyId) {}
