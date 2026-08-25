package com.vetsoftware.app.platformaccess.testsupport;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Object mother de la rodaja {@code platformaccess}.
 *
 * <p>
 * <b>Todo lo que sale de aquí es dominio de verdad, nunca un doble.</b> Un
 * {@code PlatformAccessRequest} mockeado no evalúa sus invariantes —el mínimo
 * de 20 caracteres del motivo, el hash de 64 hex, el par
 * {@code decision}/{@code decidedAt}— y dejaría pasar tests verdes sobre datos
 * que producción rechaza en el constructor.
 *
 * <p>
 * El reloj es fijo y público: los servicios de esta feature reciben
 * {@code Clock} por constructor, así que ninguna prueba necesita
 * {@code LocalDateTime.now()}.
 */
public final class PlatformAccessMother {

    /** Reloj de toda la rodaja. Fijo: la caducidad se compara, no se espera. */
    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"),
            ZoneOffset.UTC);

    public static final LocalDateTime AHORA = LocalDateTime.ofInstant(RELOJ.instant(),
            ZoneOffset.UTC);

    public static final Long ID_SOLICITUD = 4271L;
    public static final String NOMBRE = "Ana Ramirez";
    public static final String CORREO = "ana@vetrina.co";
    public static final String MOTIVO = "Necesito acceso para operar la plataforma";
    public static final String HASH_CODIGO = "$2a$10$hash-bcrypt-de-prueba";
    public static final String CODIGO = "123456";
    public static final String TOKEN_PLANO = "token-plano-de-prueba";

    private PlatformAccessMother() {
    }

    /** Un digest hex de 64 caracteres, que es lo único que el dominio acepta. */
    public static String hashDe(String semilla) {
        return String.format("%064x", Integer.toUnsignedLong(semilla.hashCode()));
    }

    public static PlatformAccessRequest solicitudPendiente() {
        return solicitud(0, null, null, AHORA.plusHours(1));
    }

    public static PlatformAccessRequest solicitudCaducada() {
        return solicitud(0, null, null, AHORA.minusMinutes(1));
    }

    public static PlatformAccessRequest solicitudBloqueada() {
        return solicitud(5, null, null, AHORA.plusHours(1));
    }

    public static PlatformAccessRequest solicitudDecidida(PlatformAccessDecision decision) {
        return solicitud(0, decision, AHORA.minusMinutes(30), AHORA.plusHours(1));
    }

    public static PlatformAccessRequest solicitud(int intentos, PlatformAccessDecision decision,
            LocalDateTime decidida, LocalDateTime expira) {
        return new PlatformAccessRequest(ID_SOLICITUD, NOMBRE, CORREO, MOTIVO, hashDe("aprobacion"),
                HASH_CODIGO, intentos, 5, expira, decision, decidida, AHORA.minusHours(1), 0L);
    }

    public static PlatformAccessInvitation invitacionViva() {
        return new PlatformAccessInvitation(88L, ID_SOLICITUD, hashDe("invitacion"),
                AHORA.plusDays(7), null, null, AHORA.minusHours(1));
    }

    public static PlatformAccessInvitation invitacionCaducada() {
        return new PlatformAccessInvitation(88L, ID_SOLICITUD, hashDe("invitacion"),
                AHORA.minusMinutes(1), null, null, AHORA.minusDays(8));
    }

    public static PlatformAccessInvitation invitacionConsumida() {
        return new PlatformAccessInvitation(88L, ID_SOLICITUD, hashDe("invitacion"),
                AHORA.plusDays(7), AHORA.minusMinutes(5), 9001L, AHORA.minusHours(1));
    }
}
