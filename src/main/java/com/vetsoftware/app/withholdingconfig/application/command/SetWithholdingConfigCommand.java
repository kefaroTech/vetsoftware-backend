package com.vetsoftware.app.withholdingconfig.application.command;

import java.math.BigDecimal;

public record SetWithholdingConfigCommand(
    BigDecimal reteFuenteRate, BigDecimal reteIvaRate, BigDecimal reteIcaRate, Long companyId) {}
