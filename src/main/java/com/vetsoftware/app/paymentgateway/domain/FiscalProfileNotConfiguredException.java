package com.vetsoftware.app.paymentgateway.domain;

/**
 * La empresa no tiene un correo fiscal vigente en el momento del cobro. A
 * diferencia de {@link PaymentGatewayNotConfiguredException} (la pasarela misma
 * está apagada), aquí la pasarela funciona pero falta un dato de la empresa:
 * cada llamador la traduce en un intento {@code CONFIGURATION}, igual que la
 * rama sin medio de pago.
 */
public class FiscalProfileNotConfiguredException extends RuntimeException {

    public FiscalProfileNotConfiguredException(Long companyId) {
        super("La empresa " + companyId + " no tiene perfil fiscal vigente");
    }
}
