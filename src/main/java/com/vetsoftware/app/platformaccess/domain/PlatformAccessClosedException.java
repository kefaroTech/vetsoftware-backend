package com.vetsoftware.app.platformaccess.domain;

/**
 * El formulario público de solicitud de acceso está cerrado (interruptor
 * {@code platform.access-request.open} en {@code system_configurations}).
 *
 * <p>
 * Sale como 404 y su {@code detail} <b>no explica el motivo</b>: el front pinta
 * texto propio para este estado y nunca muestra el mensaje del servidor. Un
 * detalle que dijese «ya existe un superadministrador» se leería en la pestaña
 * de red del navegador.
 */
public class PlatformAccessClosedException extends RuntimeException {

    public PlatformAccessClosedException(String message) {
        super(message);
    }
}
