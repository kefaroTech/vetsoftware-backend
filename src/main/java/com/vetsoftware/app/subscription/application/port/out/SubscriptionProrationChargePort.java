package com.vetsoftware.app.subscription.application.port.out;

/**
 * Solo para prorrateos positivos: el negativo dentro de la factura de ciclo
 * sigue sin resolverse (#809).
 */
public interface SubscriptionProrationChargePort {

    void chargeProration(SubscriptionProrationLine line);
}
