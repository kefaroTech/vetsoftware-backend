package com.vetsoftware.app.platformaccess.domain;

/**
 * Invariante de la contraseña que el invitado elige al aceptar.
 *
 * <p>
 * Se repite aquí lo que el {@code request} REST ya declara con {@code @Size}
 * porque son dos redes distintas: la de Bean Validation produce el error por
 * campo que el front sabe pintar bajo el input, y esta impide que un camino
 * futuro que no pase por el controller cree una cuenta de superadministrador
 * con una contraseña de cuatro caracteres.
 *
 * <p>
 * Los topes son los del front ({@code PASSWORD_MIN} / {@code PASSWORD_MAX}) y
 * no los 8 caracteres del alta ordinaria de usuarios de sistema: esta cuenta
 * tiene control total sobre todos los tenants.
 */
public final class PlatformAccessPasswords {

    public static final int PASSWORD_MIN = 12;
    public static final int PASSWORD_MAX = 100;

    private PlatformAccessPasswords() {
    }

    /**
     * @throws IllegalArgumentException
     *             si la contraseña no cumple los topes. Sale como 400 con
     *             {@code detail} constante, nunca con el valor recibido.
     */
    public static void require(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        if (rawPassword.length() < PASSWORD_MIN) {
            throw new IllegalArgumentException(
                    "password must be at least " + PASSWORD_MIN + " chars");
        }
        if (rawPassword.length() > PASSWORD_MAX) {
            throw new IllegalArgumentException(
                    "password must be " + PASSWORD_MAX + " chars or less");
        }
    }
}
