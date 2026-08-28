package com.vetsoftware.app.platformtaxprofile.domain;

import java.time.LocalDate;

/**
 * Se pidio cerrar una identidad fiscal que ya estaba cerrada.
 *
 * <p>
 * <strong>Es la capa de arriba de una defensa de tres.</strong> Debajo esta
 * {@code @Version}: si dos operadores de plataforma lanzan la sucesion a la
 * vez, los dos leen la misma fila vigente, los dos pasan por aqui, y el bloqueo
 * optimista para al segundo en el {@code UPDATE}. Y por debajo de los dos sigue
 * {@code uq_platform_tax_profiles_current} sobre la columna generada, que es lo
 * unico que el motor garantiza pase lo que pase en Java.
 */
public class PlatformTaxProfileAlreadyClosedException extends RuntimeException {

    public PlatformTaxProfileAlreadyClosedException(Long id, LocalDate validTo) {
        super("Platform tax profile " + id + " was already closed on " + validTo);
    }
}
