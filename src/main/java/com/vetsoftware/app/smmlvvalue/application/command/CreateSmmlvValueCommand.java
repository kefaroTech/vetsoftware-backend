package com.vetsoftware.app.smmlvvalue.application.command;

import java.math.BigDecimal;

/** Publica el salario minimo de un ano. Nace vigente. Solo plataforma. */
public record CreateSmmlvValueCommand(int fiscalYear, BigDecimal valueAmount,
        String legalReference) {
}
