package com.vetsoftware.app.companycontactchannel.domain;

import java.time.LocalDateTime;

/**
 * El canal ya estaba revocado y no se puede revocar otra vez.
 *
 * <p>
 * <strong>Es un conflicto y no un cuerpo mal formado</strong>: la peticion esta
 * bien escrita y lo que choca es el estado de la fila en este instante.
 *
 * <p>
 * <strong>Y no es una comprobacion cosmetica.</strong> Sin ella, la segunda
 * revocacion pisaria {@code revoked_at} y {@code revoked_reason} con la fecha
 * de hoy y con otro motivo, y esta tabla es una bitacora probatoria: el dato
 * que hay que poder ensenar es <em>cuando</em> dejo de estar autorizado el
 * canal, no cuando alguien pulso el boton por ultima vez. Reescribirlo mueve la
 * frontera entre los avisos que estaban permitidos y los que no.
 */
public class CompanyContactChannelAlreadyRevokedException extends RuntimeException {

    public CompanyContactChannelAlreadyRevokedException(Long id, LocalDateTime revokedAt) {
        super("Company contact channel " + id + " was already revoked at " + revokedAt);
    }
}
