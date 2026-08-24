package com.vetsoftware.app.pricelist.application.command;

import java.time.LocalDate;

public record CreatePriceListCommand(String code, String name, String currency, LocalDate validFrom,
        LocalDate validTo) {
}
