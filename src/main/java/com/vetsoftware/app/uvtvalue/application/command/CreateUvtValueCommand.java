package com.vetsoftware.app.uvtvalue.application.command;

import java.math.BigDecimal;

/** Publica la UVT de un ano. Solo plataforma: sin {@code companyId}. */
public record CreateUvtValueCommand(int fiscalYear, BigDecimal valueAmount, String legalReference) {
}
