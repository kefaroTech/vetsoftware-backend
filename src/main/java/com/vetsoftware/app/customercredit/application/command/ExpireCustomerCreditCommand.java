package com.vetsoftware.app.customercredit.application.command;

/**
 * Caducidad del saldo vencido de una empresa.
 *
 * <p>
 * <strong>No lleva fecha, y es a proposito:</strong> la pone el reloj inyectado
 * del servicio. Si el cliente pudiera elegir el corte, podria caducar saldo que
 * todavia esta vivo —o resucitar el que ya no lo esta— y las dos cosas mueven
 * dinero de alguien.
 */
public record ExpireCustomerCreditCommand(Long companyId) {
}
