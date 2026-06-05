package com.vetsoftware.app.tax.application.command;

import java.math.BigDecimal;

public record UpdateTaxCommand(Long id, String name, BigDecimal percentage, Long companyId) {}
