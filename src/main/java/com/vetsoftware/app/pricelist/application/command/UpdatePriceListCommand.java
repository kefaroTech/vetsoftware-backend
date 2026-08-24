package com.vetsoftware.app.pricelist.application.command;

import java.time.LocalDate;

/**
 * El codigo no viaja: es la clave de negocio de la lista y no se edita ni en
 * borrador.
 */
public record UpdatePriceListCommand(Long id, String name, String currency, LocalDate validFrom,
        LocalDate validTo) {
}
