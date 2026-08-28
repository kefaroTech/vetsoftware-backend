package com.vetsoftware.app.platformtaxprofile.domain;

/**
 * VetSoftware no tiene identidad fiscal vigente: {@code platform_tax_profiles}
 * no tiene ninguna fila con {@code valid_to} nulo.
 *
 * <h2>Esto no es un caso raro: hoy es el estado normal</h2>
 *
 * <p>
 * <strong>La tabla nace sin sembrar, a proposito.</strong> El changeset 367 lo
 * escribe con todas las letras: no habia razon social ni NIT reales de
 * VetSoftware, y no se inventaron. Una identidad fiscal inventada <em>acaba
 * impresa en la factura de cada cliente</em>, y ese error ya no es del
 * software. La fila inicial la siembra quien tenga la razon social y el NIT
 * reales; hasta entonces, cualquier lectura de «la vigente» llega aqui.
 *
 * <h2>Por que se falla en voz alta en vez de devolver vacio</h2>
 *
 * <p>
 * Es la misma decision que {@code PlatformBillingConfigNotConfiguredException}
 * toma para {@code platform_billing_config} (255): <strong>es preferible que la
 * operacion falle ruidosamente a que se emita con datos falsos o en
 * blanco</strong>. Un {@code Optional} vacio invitaria al llamador a seguir con
 * un valor por defecto, y el valor por defecto de una razon social no existe.
 *
 * <p>
 * El mensaje nombra la tabla y dice que la siembra es una decision del dueño y
 * no un despliegue pendiente: quien lo lea a las tres de la mañana tiene que
 * saber que no hay nada que reiniciar.
 */
public class NoCurrentPlatformTaxProfileException extends RuntimeException {

    public NoCurrentPlatformTaxProfileException() {
        super("VetSoftware has no platform tax profile in force: platform_tax_profiles has no row"
                + " with valid_to null. The table is deliberately unseeded (changeset 367) because"
                + " the real legal name and tax id were not available; seeding it is the owner's"
                + " decision, not a pending deployment. Nothing here is retryable");
    }
}
