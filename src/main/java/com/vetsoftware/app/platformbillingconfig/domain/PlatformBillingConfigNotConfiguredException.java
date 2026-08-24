package com.vetsoftware.app.platformbillingconfig.domain;

/**
 * La fila única de {@code platform_billing_config} no existe.
 *
 * <p>
 * <b>No es un caso de negocio, es un fallo de arranque.</b> Sin esa fila el
 * sistema no sabe cuántos días de gracia conceder tras un vencimiento, qué día
 * del mes emitir los cobros ni a cuántos días vence una factura: no hay valor
 * por defecto que inventar, porque el propósito entero de la tabla es que esas
 * políticas no vivan escritas en el código. Por eso la lectura nunca devuelve
 * {@code Optional.empty()} en silencio ni cae a constantes internas: revienta
 * con el remedio escrito en el mensaje.
 *
 * <p>
 * Se espera que el {@code GlobalExceptionHandler} la mapee a <b>500 Internal
 * Server Error</b> con código {@code PLATFORM_BILLING_CONFIG_NOT_CONFIGURED}:
 * es un despliegue incompleto, no un error del cliente, y ningún reintento lo
 * arregla (por eso tampoco es un 503). El mensaje no contiene datos de ningún
 * usuario, así que es seguro devolverlo como detalle.
 */
public class PlatformBillingConfigNotConfiguredException extends RuntimeException {

    public PlatformBillingConfigNotConfiguredException() {
        super("No existe la fila de configuración de facturación de la plataforma"
                + " (platform_billing_config). Sin ella el sistema no puede decidir los días de"
                + " gracia, el día de emisión de los cobros ni el plazo de pago. Es una tabla de"
                + " una sola fila y debe sembrarse en el mismo changeset que la crea:"
                + " INSERT INTO platform_billing_config (singleton, default_price_list_id,"
                + " default_grace_days, default_trial_days, invoice_day_of_month,"
                + " default_payment_term_days, external_billing_provider, created_date, version)"
                + " VALUES (1, NULL, 5, 14, 1, 5, NULL, NOW(), 0);");
    }
}
