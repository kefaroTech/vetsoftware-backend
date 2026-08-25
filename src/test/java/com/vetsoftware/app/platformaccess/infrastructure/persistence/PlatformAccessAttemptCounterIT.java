package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.port.in.ApprovePlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.RejectPlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessBlockedException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessCodeMismatchException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import com.vetsoftware.app.testsupport.AbstractFullApplicationIT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * <b>El contador de intentos sobrevive al rollback — y no hay forma de probarlo
 * con dobles.</b>
 *
 * <p>
 * {@code PlatformAccessDecisions.registerFailedAttempt} gasta un intento y
 * <i>acto seguido</i> lanza el 422 o el 429. Esa excepción revierte la
 * transacción del caso de uso, así que el incremento solo sobrevive porque
 * {@code PlatformAccessRequestJpaRepository.registerFailedAttempt} está
 * declarado {@code @Transactional(propagation = REQUIRES_NEW)} y confirma por
 * su cuenta. Quitar esa propagación no rompe ni un test de servicio con dobles
 * —el doble devuelve 1 igual— y deja <b>fuerza bruta libre sobre 10⁶
 * códigos</b>: el atacante prueba, recibe 422, y el contador vuelve a cero
 * solo.
 *
 * <p>
 * Por eso este caso levanta la aplicación entera contra MySQL real, invoca el
 * caso de uso de verdad y lee la fila con {@code JdbcTemplate} <b>fuera</b> de
 * cualquier contexto de persistencia: ni caché de primer nivel ni transacción
 * de test que pueda enmascarar el resultado.
 *
 * <p>
 * De regalo comprueba lo que el {@code PlatformAccessRequestPersistenceIT} no
 * puede: que el {@code REQUIRES_NEW} <b>no se bloquea contra su propia
 * transacción exterior</b>. En producción el caso de uso llega aquí tras un
 * {@code SELECT} de lectura consistente, que bajo REPEATABLE READ no toma
 * bloqueos; si alguien añadiera un {@code FOR UPDATE} o una escritura previa
 * sobre la misma fila, este test moriría por {@code Lock wait timeout} en vez
 * de pasar en silencio.
 *
 * <p>
 * <b>Datos propios que nadie más mira</b>, como exige el javadoc de la clase
 * base: correos y tokens exclusivos de este archivo, y borrado explícito al
 * terminar.
 */
@DisplayName("El contador de intentos sobrevive al rollback del 422 y del 429")
class PlatformAccessAttemptCounterIT extends AbstractFullApplicationIT {

    private static final String CODIGO_BUENO = "123456";
    private static final String CODIGO_MALO = "999999";
    private static final String MOTIVO = "Necesito acceso para operar la plataforma";
    private static final String CORREO_422 = "contador-rollback-422@vetrina.invalid";
    private static final String CORREO_429 = "contador-rollback-429@vetrina.invalid";
    private static final String CORREO_RECHAZO = "contador-rollback-reject@vetrina.invalid";

    @Autowired
    private PlatformAccessRequestRepository requestRepository;
    @Autowired
    private ApprovePlatformAccessRequestUseCase approveUseCase;
    @Autowired
    private RejectPlatformAccessRequestUseCase rejectUseCase;
    @Autowired
    private SecretHasherPort secretHasher;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void borrarLasFilasDeEsteArchivo() {
        jdbcTemplate.update("DELETE FROM platform_access_requests WHERE email IN (?, ?, ?)",
                CORREO_422, CORREO_429, CORREO_RECHAZO);
    }

    /**
     * El mismo SHA-256 hex que {@code PlatformAccessTokens.hash}, que es
     * package-private de {@code application.usecase} y no se puede importar desde
     * aquí. Escribirlo a mano es además lo correcto: si alguien cambiara el digest,
     * este test debe caer.
     */
    private static String hashDelToken(String rawToken) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    }

    private PlatformAccessRequest emitir(String email, String rawToken) throws Exception {
        LocalDateTime creada = LocalDateTime.now().minusMinutes(5);
        return requestRepository.save(
                PlatformAccessRequest.issue("Ana Ramirez", email, MOTIVO, hashDelToken(rawToken),
                        secretHasher.hash(CODIGO_BUENO), 5, creada, creada.plusHours(72)));
    }

    private int intentosEnLaBase(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT verification_attempts FROM platform_access_requests WHERE id = ?",
                Integer.class, id);
    }

    private void fallarConCodigoIncorrecto(String rawToken, int veces) {
        IntStream.range(0, veces)
                .forEach(i -> assertThatThrownBy(() -> approveUseCase
                        .execute(new ResolvePlatformAccessCommand(rawToken, CODIGO_MALO)))
                        .isInstanceOf(RuntimeException.class));
    }

    @Nested
    @DisplayName("aprobar")
    class Aprobar {

        @Test
        @DisplayName("un codigo incorrecto deja el contador en 1 aunque la transaccion del caso de uso revierta")
        void un_codigo_incorrecto_deja_el_contador_en_uno() throws Exception {
            PlatformAccessRequest solicitud = emitir(CORREO_422, "token-rollback-422");

            assertThatThrownBy(() -> approveUseCase
                    .execute(new ResolvePlatformAccessCommand("token-rollback-422", CODIGO_MALO)))
                    .isInstanceOf(PlatformAccessCodeMismatchException.class)
                    .hasMessageContaining("Verification code does not match");

            // Con REQUIRED en vez de REQUIRES_NEW esto vale 0 y el 422 sale gratis:
            // 10^6 combinaciones sin techo.
            assertThat(intentosEnLaBase(solicitud.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("cinco fallos agotan el margen y el sexto sale 429 con la fila en el tope")
        void cinco_fallos_agotan_el_margen() throws Exception {
            PlatformAccessRequest solicitud = emitir(CORREO_429, "token-rollback-429");

            fallarConCodigoIncorrecto("token-rollback-429", 5);

            assertThat(intentosEnLaBase(solicitud.getId())).isEqualTo(5);
            // El bloqueo es terminal: ni el codigo correcto lo revierte.
            assertThatThrownBy(() -> approveUseCase
                    .execute(new ResolvePlatformAccessCommand("token-rollback-429", CODIGO_BUENO)))
                    .isInstanceOf(PlatformAccessBlockedException.class);
            assertThat(intentosEnLaBase(solicitud.getId())).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("rechazar comparte contador con aprobar")
    class Rechazar {

        @Test
        @DisplayName("un codigo incorrecto en /reject gasta el mismo intento y tambien sobrevive al rollback")
        void un_codigo_incorrecto_en_reject_gasta_intento() throws Exception {
            PlatformAccessRequest solicitud = emitir(CORREO_RECHAZO, "token-rollback-reject");

            // Quien puede rechazar puede aprobar: relajar el contador del rechazo
            // daria un canal para quemar solicitudes ajenas a coste cero.
            assertThatThrownBy(() -> rejectUseCase.execute(
                    new ResolvePlatformAccessCommand("token-rollback-reject", CODIGO_MALO)))
                    .isInstanceOf(PlatformAccessCodeMismatchException.class);

            assertThat(intentosEnLaBase(solicitud.getId())).isEqualTo(1);
        }
    }
}
