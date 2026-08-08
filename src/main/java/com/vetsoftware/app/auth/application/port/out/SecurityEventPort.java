package com.vetsoftware.app.auth.application.port.out;

/**
 * Salida de eventos de seguridad que nacen en la capa de aplicación.
 *
 * <p>
 * Existe por la regla de dependencias: {@code AuditLogger} vive en
 * {@code infrastructure} y {@code application} no puede importarlo. La regla
 * está congelada en ArchUnit, así que un import directo no solo sería
 * incorrecto — rompería el build.
 */
public interface SecurityEventPort {

    /**
     * Se presentó un refresh token ya revocado. Según el <em>OAuth 2.0 Security
     * Best Current Practice</em> (§4.14.2) esa es la señal canónica de que el token
     * fue robado: el legítimo y el atacante no pueden usar el mismo token una sola
     * vez cada uno.
     *
     * @param subjectId
     *            identificador del sujeto cuya familia de tokens se revocó
     * @param subjectType
     *            {@code EMPLOYEE} o {@code SYSTEM_USER}
     * @param secondsSinceRevocation
     *            antigüedad de la revocación previa; sirve para distinguir un robo
     *            de una carrera entre pestañas al revisar el log
     */
    void refreshTokenReuseDetected(Long subjectId, String subjectType, long secondsSinceRevocation);
}
