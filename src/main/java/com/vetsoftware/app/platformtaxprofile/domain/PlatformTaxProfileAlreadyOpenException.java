package com.vetsoftware.app.platformtaxprofile.domain;

import java.time.LocalDate;

/**
 * Se pidio abrir la primera identidad fiscal cuando ya hay una vigente.
 *
 * <p>
 * <strong>Es una traduccion, no una garantia.</strong> Lo unico que impide de
 * verdad que existan dos identidades vigentes a la vez es
 * {@code uq_platform_tax_profiles_current} sobre la columna generada: entre la
 * lectura del caso de uso y su {@code INSERT} cabe otra transaccion. Lo que
 * esta excepcion aporta es que el caso comun —el boton pulsado dos veces, o
 * quien busca «crear» cuando lo que toca es «suceder»— conteste un 409 que
 * nombra la ficha vigente y remite a la sucesion, en vez de un 500 con un
 * {@code Duplicate entry} sobre una columna que no aparece en ningun sitio del
 * codigo Java.
 *
 * <p>
 * El mensaje dice desde cuando rige la vigente porque esa fecha es la que
 * decide si la sucesion es representable hoy o como pronto mañana: ver
 * {@link PlatformTaxProfileSuccessionNotAfterCurrentException}.
 */
public class PlatformTaxProfileAlreadyOpenException extends RuntimeException {

    public PlatformTaxProfileAlreadyOpenException(Long id, LocalDate validFrom) {
        super("VetSoftware already has a platform tax profile in force (id " + id + ", since "
                + validFrom + "): change it with a succession, which closes the current one and"
                + " opens the next in a single transaction, instead of opening a second one");
    }
}
