package com.vetsoftware.app.platformtaxprofile.domain;

/**
 * No existe la identidad fiscal pedida por id.
 *
 * <p>
 * Se distingue de {@link NoCurrentPlatformTaxProfileException} a proposito, y
 * la diferencia importa: esta dice «ese id no esta en el historico», que es un
 * 404 corriente de una consola; aquella dice «VetSoftware no tiene identidad
 * fiscal vigente», que es un fallo de configuracion del producto entero.
 * Fundirlas en una sola haria que el segundo caso —el grave— llegara disfrazado
 * del primero.
 */
public class PlatformTaxProfileNotFoundException extends RuntimeException {

    public PlatformTaxProfileNotFoundException(Long id) {
        super("Platform tax profile not found: " + id);
    }
}
