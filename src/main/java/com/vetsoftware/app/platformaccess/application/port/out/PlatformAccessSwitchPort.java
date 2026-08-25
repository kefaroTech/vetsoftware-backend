package com.vetsoftware.app.platformaccess.application.port.out;

/**
 * Interruptor del formulario publico. Vive como una fila de
 * {@code system_configurations}, no como una tabla propia ni como una propiedad
 * del despliegue: hay que poder cerrarlo en caliente, sin desplegar, porque es
 * tambien el interruptor de emergencia.
 *
 * <p>
 * Ante un valor ausente o ilegible el formulario se considera <b>cerrado</b>:
 * fallo seguro.
 */
public interface PlatformAccessSwitchPort {

    boolean isOpen();
}
