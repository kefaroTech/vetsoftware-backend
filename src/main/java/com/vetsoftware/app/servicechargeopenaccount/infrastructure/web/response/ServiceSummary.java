package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response;

import java.math.BigDecimal;

public record ServiceSummary(Long id, String name, BigDecimal price) {}
