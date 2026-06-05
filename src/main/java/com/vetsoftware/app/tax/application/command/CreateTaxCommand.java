package com.vetsoftware.app.tax.application.command;

import java.math.BigDecimal;

public record CreateTaxCommand(String name, BigDecimal percentage, Long companyId) {}
