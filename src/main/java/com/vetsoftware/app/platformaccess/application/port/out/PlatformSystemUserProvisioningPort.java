package com.vetsoftware.app.platformaccess.application.port.out;

import java.time.LocalDateTime;

/**
 * Alta del usuario de sistema que resulta de aceptar la invitacion.
 *
 * <p>
 * Es un puerto propio y no un import de {@code systemuser.domain.SystemUser}:
 * el vertical slicing prohibe cruzar dominios entre features. El unico cruce
 * permitido vive en el adaptador, dentro de {@code infrastructure/persistence}.
 *
 * <p>
 * Las dos comprobaciones de disponibilidad son <b>internas</b> y solo se
 * ejecutan con un token de invitacion valido en la mano. No existe —ni debe
 * existir— ningun endpoint publico de "esta libre este correo".
 */
public interface PlatformSystemUserProvisioningPort {

    /**
     * {@code true} si ya hay una cuenta de sistema con ese correo, activa o dada de
     * baja. Incluir las dadas de baja es deliberado: un superadministrador inactivo
     * retiene su correo, y el camino correcto para su vuelta es reactivarlo, no
     * crear un segundo usuario con la misma identidad.
     */
    boolean emailTaken(String email);

    /** {@code true} si el codigo de login ya esta tomado, activo o no. */
    boolean codeTaken(String code);

    /** @return el id del usuario de sistema creado. */
    Long provision(String code, String email, String fullName, String hashedPassword,
            LocalDateTime createdDate);
}
