package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Como cobra una linea del contrato. Es el <b>unico</b> criterio con el que
 * este slice decide si algo devenga: {@code PAID} devenga, los otros tres
 * jamas.
 *
 * <p>
 * <b>Companion VO, no un import.</b> Espeja la columna
 * {@code subscription_items.charge_mode} ({@code VARCHAR(20)},
 * {@code NOT NULL}, defecto {@code 'PAID'}) y vive aqui porque el vertical
 * slicing prohibe importar el enum equivalente de otra feature. El valor se
 * resuelve en el adaptador, contra la tabla, y entra al dominio ya traducido.
 *
 * <p>
 * <b>El filtro es por linea, nunca por contrato</b> (R-TRIAL-13). Un contrato
 * en {@code TRIALING} puede tener lineas {@code PAID} --la facturacion
 * electronica DIAN se cobra desde el dia 0 aunque el resto del contrato este en
 * prueba--, asi que descartar el contrato entero por su estado deja de facturar
 * servicios realmente prestados. En este slice no hay ni un metodo que mire el
 * estado del contrato, y es deliberado: si aparece uno, la regla se rompio.
 *
 * <p>
 * <b>La linea gratuita guarda su precio real, y por eso la omision se paga
 * cara</b> (R-TRIAL-14). Una linea {@code TRIAL} o {@code FREE_LIMITED}
 * <b>no</b> lleva {@code unit_amount = 0}: conserva la tarifa que se cobrara
 * cuando la prueba termine, para que la conversion no tenga que reconstruir
 * precios ni volver al catalogo del dia de la firma. La consecuencia es que una
 * consulta de cobro que filtre solo por vigencia y olvide {@code charge_mode}
 * <b>no devuelve ceros: devuelve la tarifa completa</b> y le cobra a todos los
 * clientes en prueba. No hay ninguna senal intermedia que lo delate -- el
 * importe se ve perfectamente normal en la factura.
 */
public enum ItemChargeMode {

    /** En prueba. No devenga: el precio guardado es el que se cobrara despues. */
    TRIAL,
    /** La unica que devenga. */
    PAID,
    /** Gratis con tope. No devenga: lo que pasa del tope se corta, no se cobra. */
    FREE_LIMITED,
    /** Vencida y en solo lectura. No devenga: ya no se presta el servicio. */
    EXPIRED_READ_ONLY;

    /** {@code true} solo para {@link #PAID}. */
    public boolean generatesCharge() {
        return this == PAID;
    }

    /**
     * Traduce el texto crudo de la columna.
     *
     * <p>
     * <b>Un valor desconocido revienta y no se degrada a {@code PAID}.</b>
     * Degradarlo a {@code PAID} seria cobrarle a alguien por un modo que este
     * codigo no entiende; degradarlo a "no cobra" seria dejar de facturar en
     * silencio. Las dos son peores que parar el cierre con un mensaje que nombra el
     * valor.
     */
    public static ItemChargeMode de(String value) {
        if (value == null || value.isBlank())
            throw new IllegalStateException("subscription_items.charge_mode is empty:"
                    + " billing cannot decide whether that line accrues");
        try {
            return valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Unknown subscription_items.charge_mode '" + value
                    + "': billing cannot decide whether that line accrues", exception);
        }
    }
}
