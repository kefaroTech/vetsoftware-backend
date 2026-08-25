package com.vetsoftware.app.infrastructure.audit;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

/**
 * El logger {@code AUDIT} es el <b>único</b> destino de los eventos de
 * auditoría desde que se retiró el outbox, así que estas pruebas se afirman
 * sobre lo que sale por él: el nombre del evento, el resultado y los campos
 * estructurados de cada hecho.
 *
 * <p>
 * Que un campo se emita no garantiza que llegue legible a Grafana — la
 * allowlist de {@code LogFieldPolicy} puede enmascararlo sin romper nada. Esa
 * es la contra-prueba de {@code AuditFieldsSurviveRedactionTest}, y las dos
 * juntas cubren emisión y visibilidad.
 */
@DisplayName("AuditLogger")
class AuditLoggerTest {

    private final AuditLogger logger = new AuditLogger();

    private Logger auditChannel;
    private ListAppender<ILoggingEvent> sink;
    private Level previousLevel;

    @BeforeEach
    void wireAuditChannel() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        sink = new ListAppender<>();
        sink.setContext(context);
        sink.start();

        auditChannel = context.getLogger("AUDIT");
        previousLevel = auditChannel.getLevel();
        auditChannel.setLevel(Level.INFO);
        auditChannel.addAppender(sink);
    }

    @AfterEach
    void tearDown() {
        auditChannel.detachAppender(sink);
        auditChannel.setLevel(previousLevel);
        sink.stop();
    }

    /**
     * Único evento emitido; falla con mensaje claro si se emitió cero o más de uno.
     */
    private ILoggingEvent emitted() {
        assertThat(sink.list).as("se esperaba exactamente un evento AUDIT").hasSize(1);
        return sink.list.get(0);
    }

    private Map<String, Object> fields() {
        List<KeyValuePair> pairs = emitted().getKeyValuePairs();
        Map<String, Object> fields = new LinkedHashMap<>();
        if (pairs != null) {
            pairs.forEach(pair -> fields.put(pair.key, pair.value));
        }
        return fields;
    }

    @Nested
    @DisplayName("mutation")
    class Mutation {

        @Test
        @DisplayName("emite evento, estado, resultado y duracion; metodo y ruta van en el mensaje")
        void emite_estado_resultado_y_duracion() {
            logger.mutation("POST", "/api/v1/animals", 201, "SUCCESS", 42L);

            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields()).isEqualTo(Map.of("event", "http_mutation", "http.status", 201,
                    "outcome", "SUCCESS", "http.durationMs", 42L));
            assertThat(emitted().getFormattedMessage())
                    .isEqualTo("mutation POST /api/v1/animals -> 201 (SUCCESS)");
        }
    }

    @Nested
    @DisplayName("registro e invitaciones")
    class RegistroEInvitaciones {

        @Test
        @DisplayName("companyRegistered emite los datos de la empresa y del dueño que la registro")
        void company_registered_emite_los_datos_de_la_empresa() {
            logger.companyRegistered(1L, "Vet Uno", "900123456-7", 5L, "OWNER-01");

            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields()).isEqualTo(Map.of("event", "company_registered", "company.id", 1L,
                    "company.name", "Vet Uno", "company.identifier", "900123456-7",
                    "actor.employeeId", 5L, "actor.identifier", "OWNER-01", "outcome", "SUCCESS"));
        }

        @Test
        @DisplayName("employeeInvited emite el empleado invitado y la empresa")
        void employee_invited_emite_el_empleado_invitado() {
            logger.employeeInvited(9L, "EMP-09", 1L);

            assertThat(fields()).isEqualTo(Map.of("event", "employee_invited", "employee.id", 9L,
                    "employee.identifier", "EMP-09", "company.id", 1L, "outcome", "SUCCESS"));
        }

        @Test
        @DisplayName("employeeInvitationResent emite el reenvio de la invitacion")
        void employee_invitation_resent_emite_el_reenvio() {
            logger.employeeInvitationResent(9L, "EMP-09", 1L);

            assertThat(fields()).isEqualTo(Map.of("event", "employee_invitation_resent",
                    "employee.id", 9L, "employee.identifier", "EMP-09", "company.id", 1L, "outcome",
                    "SUCCESS"));
        }

        @Test
        @DisplayName("invitationAccepted emite el empleado que activo su cuenta")
        void invitation_accepted_emite_el_empleado_activado() {
            logger.invitationAccepted(9L, 1L);

            assertThat(fields()).isEqualTo(Map.of("event", "invitation_accepted", "employee.id", 9L,
                    "company.id", 1L, "outcome", "SUCCESS"));
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("loginSuccess emite el tipo y el identificador del actor")
        void login_success_emite_el_tipo_y_el_identificador() {
            logger.loginSuccess("EMPLOYEE", "EMP-09");

            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields()).isEqualTo(Map.of("event", "login_success", "actor.type",
                    "EMPLOYEE", "actor.identifier", "EMP-09", "outcome", "SUCCESS"));
        }

        @Test
        @DisplayName("loginFailure emite el motivo en WARN y la ruta en el mensaje")
        void login_failure_emite_el_motivo() {
            logger.loginFailure("/api/v1/auth/login", "bad_credentials");

            assertThat(emitted().getLevel()).isEqualTo(Level.WARN);
            assertThat(fields()).isEqualTo(Map.of("event", "login_failure", "outcome", "FAILURE",
                    "reason", "bad_credentials"));
            assertThat(emitted().getFormattedMessage())
                    .isEqualTo("login failure /api/v1/auth/login reason=bad_credentials");
        }

        @Test
        @DisplayName("loginBlockedEmailNotVerified emite el identificador bloqueado")
        void login_blocked_email_not_verified_emite_el_identificador() {
            logger.loginBlockedEmailNotVerified("EMP-09");

            assertThat(emitted().getLevel()).isEqualTo(Level.WARN);
            assertThat(fields()).isEqualTo(
                    Map.of("event", "login_blocked_email_not_verified", "actor.identifier",
                            "EMP-09", "outcome", "DENIED", "reason", "email_not_verified"));
        }

        @Test
        @DisplayName("refreshTokenReuseDetected emite el sujeto y el tiempo desde la revocacion")
        void refresh_token_reuse_detected_emite_el_sujeto_y_el_tiempo() {
            logger.refreshTokenReuseDetected(9L, "EMPLOYEE", 30L);

            assertThat(emitted().getLevel()).isEqualTo(Level.WARN);
            assertThat(fields()).isEqualTo(
                    Map.of("event", "refresh_token_reuse_detected", "actor.id", 9L, "actor.type",
                            "EMPLOYEE", "outcome", "DENIED", "seconds_since_revocation", 30L));
        }
    }

    @Nested
    @DisplayName("autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("accessDenied emite el metodo y la ruta denegados en el mensaje")
        void access_denied_emite_metodo_y_ruta() {
            logger.accessDenied("DELETE", "/api/v1/animals/1");

            assertThat(emitted().getLevel()).isEqualTo(Level.WARN);
            assertThat(fields()).isEqualTo(Map.of("event", "access_denied", "outcome", "DENIED"));
            assertThat(emitted().getFormattedMessage())
                    .isEqualTo("access denied DELETE /api/v1/animals/1");
        }

        /**
         * El motivo de ejemplo es uno de los cuatro del vocabulario cerrado real
         * —{@code token_missing}, {@code token_expired}, {@code token_invalid},
         * {@code session_replaced}— y no un literal inventado: quien lea este test como
         * documentación del campo {@code reason} tiene que ver el valor que sale en
         * producción.
         *
         * <p>
         * El nivel es <b>INFO y no WARN</b>: un 401 por token ausente o caducado es el
         * desenlace de diseño de una API con sesiones —cualquiera puede provocarlo a
         * voluntad—, y emitirlo a WARN enterraba bajo su volumen los eventos del canal
         * que sí piden revisión humana ({@code access_denied}, {@code rate_limited},
         * {@code refresh_token_reuse_detected}).
         */
        @Test
        @DisplayName("unauthenticated emite el motivo a nivel INFO y deja metodo y ruta en el mensaje")
        void unauthenticated_emite_motivo() {
            logger.unauthenticated("GET", "/api/v1/animals", "token_missing");

            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields()).isEqualTo(Map.of("event", "unauthenticated", "outcome", "DENIED",
                    "reason", "token_missing"));
            assertThat(emitted().getFormattedMessage())
                    .isEqualTo("unauthenticated GET /api/v1/animals reason=token_missing");
        }
    }

    @Nested
    @DisplayName("rate limiting")
    class RateLimiting {

        @Test
        @DisplayName("loginRateLimited emite el codigo LOGIN_RATE_LIMITED")
        void login_rate_limited_emite_el_codigo() {
            logger.loginRateLimited();

            assertThat(emitted().getLevel()).isEqualTo(Level.WARN);
            assertThat(fields()).isEqualTo(Map.of("event", "rate_limited", "code",
                    "LOGIN_RATE_LIMITED", "outcome", "DENIED"));
        }

        @Test
        @DisplayName("rateLimited emite el codigo recibido")
        void rate_limited_emite_el_codigo_recibido() {
            logger.rateLimited("PUBLIC_ROUTE_RATE_LIMITED");

            assertThat(fields()).isEqualTo(Map.of("event", "rate_limited", "code",
                    "PUBLIC_ROUTE_RATE_LIMITED", "outcome", "DENIED"));
        }
    }

    @Nested
    @DisplayName("alta de superadministradores de plataforma (#360)")
    class AltaDeSuperadministradores {

        private static final Long SOLICITUD = 4271L;

        @Test
        @DisplayName("la solicitud recibida emite el dominio del correo y NADA del solicitante")
        void la_solicitud_recibida_emite_solo_el_dominio() {
            logger.systemUserRequested(SOLICITUD, "vetrina.co");

            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            // Ni nombre, ni motivo, ni la direccion entera: son datos personales de
            // alguien que quiza nunca fue aprobado y no consintio nada. El dominio
            // es lo unico que responde una pregunta operativa —cuarenta dominios
            // desechables o tres personas de la misma empresa—.
            assertThat(fields())
                    .isEqualTo(Map.of("event", "system_user_requested", "system.user.request.id",
                            SOLICITUD, "email.domain", "vetrina.co", "outcome", "SUCCESS"));
            assertThat(emitted().getFormattedMessage()).doesNotContain("@")
                    .doesNotContain("vetrina.co");
        }

        @Test
        @DisplayName("el formulario cerrado se registra aqui, que es el unico sitio donde consta el motivo")
        void el_formulario_cerrado_se_registra_con_su_motivo() {
            logger.systemUserRequestDenied("form_closed", null, "vetrina.co");

            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields()).containsEntry("event", "system_user_request_denied")
                    .containsEntry("reason", "form_closed").containsEntry("outcome", "DENIED");
        }

        @Test
        @DisplayName("la solicitud duplicada apunta a la solicitud viva que ya existia")
        void la_solicitud_duplicada_apunta_a_la_viva() {
            logger.systemUserRequestDenied("duplicate_request", SOLICITUD, "vetrina.co");

            assertThat(fields()).containsEntry("reason", "duplicate_request")
                    .containsEntry("system.user.request.id", SOLICITUD);
        }

        @Test
        @DisplayName("el token invalido sale en INFO y sin id: no hay solicitud a la que atribuirlo")
        void el_token_invalido_sale_en_info_y_sin_id() {
            logger.systemUserApprovalDenied("token_invalid", null);

            // INFO y no WARN: cualquier anonimo puede provocarlo a voluntad y el
            // sistema funciono como debia. Lo que importa es la tasa, y la tasa es
            // una metrica.
            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields()).containsEntry("event", "system_user_approval_denied")
                    .containsEntry("reason", "token_invalid").containsEntry("outcome", "DENIED");
        }

        @Test
        @DisplayName("el token caducado reutiliza el vocabulario ya vivo token_expired")
        void el_token_caducado_reutiliza_el_vocabulario_vivo() {
            logger.systemUserApprovalDenied("token_expired", SOLICITUD);

            // No «approval_token_expired»: un vocabulario paralelo impide preguntar
            // «cuantos rechazos por token caducado hubo hoy» en todo el sistema.
            assertThat(fields()).containsEntry("reason", "token_expired");
        }

        @Test
        @DisplayName("la reproduccion del token sale en WARN con los segundos desde el consumo")
        void la_reproduccion_sale_en_warn_con_los_segundos() {
            logger.systemUserApprovalReplayed(SOLICITUD, 86_400L);

            // WARN, misma semantica que refresh_token_reuse_detected: describe un
            // ataque en curso, no una decision rutinaria. Los segundos separan el
            // doble clic del aprobador de la reproduccion de un correo filtrado.
            assertThat(emitted().getLevel()).isEqualTo(Level.WARN);
            assertThat(fields()).containsEntry("event", "system_user_approval_denied")
                    .containsEntry("reason", "token_consumed")
                    .containsEntry("seconds_since_consumption", 86_400L);
        }

        @Test
        @DisplayName("el codigo incorrecto emite el margen restante y NUNCA el codigo")
        void el_codigo_incorrecto_emite_el_margen_y_nunca_el_codigo() {
            logger.systemUserApprovalCodeMismatch(SOLICITUD, 3);

            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields()).containsEntry("reason", "code_mismatch")
                    .containsEntry("attempts.remaining", 3);
            // LogRedactor solo suprime corridas de 10 digitos o mas: un codigo de 6
            // saldria entero y no hay red que lo pare. La garantia es que el evento
            // no tiene donde meterlo, ni como campo ni en el mensaje.
            assertThat(fields()).doesNotContainKey("code").doesNotContainKey("verification.code");
            assertThat(emitted().getFormattedMessage()).doesNotContainPattern("\\d{6}");
        }

        @Test
        @DisplayName("los intentos agotados salen en WARN: alguien prueba codigos y un aprobador quedo fuera")
        void los_intentos_agotados_salen_en_warn() {
            logger.systemUserApprovalLocked(SOLICITUD);

            assertThat(emitted().getLevel()).isEqualTo(Level.WARN);
            assertThat(fields()).isEqualTo(
                    Map.of("event", "system_user_approval_locked", "reason", "attempts_exhausted",
                            "system.user.request.id", SOLICITUD, "outcome", "DENIED"));
        }

        @Test
        @DisplayName("aprobar y rechazar emiten dos eventos distintos, no uno con bandera")
        void aprobar_y_rechazar_emiten_dos_eventos_distintos() {
            logger.systemUserRequestApproved(SOLICITUD);

            assertThat(fields()).isEqualTo(Map.of("event", "system_user_request_approved",
                    "system.user.request.id", SOLICITUD, "outcome", "SUCCESS"));
        }

        @Test
        @DisplayName("el rechazo tiene su propio evento y su outcome es SUCCESS: el sistema hizo lo pedido")
        void el_rechazo_tiene_su_propio_evento() {
            logger.systemUserRequestRejected(SOLICITUD);

            assertThat(fields()).isEqualTo(Map.of("event", "system_user_request_rejected",
                    "system.user.request.id", SOLICITUD, "outcome", "SUCCESS"));
        }

        @Test
        @DisplayName("la invitacion enviada no lleva el token ni el enlace")
        void la_invitacion_enviada_no_lleva_el_token() {
            logger.systemUserInvited(SOLICITUD, "vetrina.co");

            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields())
                    .isEqualTo(Map.of("event", "system_user_invited", "system.user.request.id",
                            SOLICITUD, "email.domain", "vetrina.co", "outcome", "SUCCESS"));
        }

        @Test
        @DisplayName("el correo de invitacion perdido es un ERROR: no hay reintento ni outbox")
        void el_correo_perdido_es_el_unico_error_del_flujo() {
            logger.systemUserInvitationUndelivered(SOLICITUD, "vetrina.co");

            // ERROR porque no hay reintento ni outbox: el mensaje se perdio
            // definitivamente aunque Resend respondiera 200, y nadie lo recupera sin
            // que una persona reemita la invitacion.
            assertThat(emitted().getLevel()).isEqualTo(Level.ERROR);
            assertThat(fields()).containsEntry("event", "system_user_invitation_undelivered")
                    .containsEntry("reason", "email_failed").containsEntry("outcome", "FAILURE");
            assertThat(emitted().getFormattedMessage()).contains("reissue it manually");
        }

        @Test
        @DisplayName("el correo de bienvenida perdido es el segundo ERROR, y un hecho distinto")
        void el_correo_de_bienvenida_perdido_es_su_propio_error() {
            logger.systemUserWelcomeUndelivered(SOLICITUD, "vetrina.co");

            // Mismo motivo que el de la invitacion: sin reintento ni outbox, el
            // mensaje se perdio. Pero el arreglo es otro —el codigo sigue en
            // system_users y se puede comunicar a mano, no hay que reemitir nada—,
            // asi que mezclarlos en un solo evento haria imposible saber cual toca.
            assertThat(emitted().getLevel()).isEqualTo(Level.ERROR);
            assertThat(fields()).containsEntry("event", "system_user_welcome_undelivered")
                    .containsEntry("reason", "email_failed").containsEntry("outcome", "FAILURE");
            assertThat(emitted().getFormattedMessage()).contains("cannot sign in");
        }

        @Test
        @DisplayName("aceptar con un correo que ya tiene cuenta deja el UNICO rastro del intento")
        void aceptar_con_un_correo_ya_tomado_deja_rastro() {
            logger.systemUserInvitationDenied("email_already_provisioned", SOLICITUD);

            // La respuesta es un 404 indistinguible de un token muerto, a proposito.
            // Sin este evento el intento no ocurre en ningun registro del sistema —y
            // para provocarlo hay que poseer una invitacion valida—.
            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields()).isEqualTo(Map.of("event", "system_user_invitation_denied",
                    "reason", "email_already_provisioned", "system.user.request.id", SOLICITUD,
                    "outcome", "DENIED"));
        }

        @Test
        @DisplayName("el token de invitacion inexistente sale sin id, como su gemelo del aprobador")
        void el_token_de_invitacion_inexistente_sale_sin_id() {
            logger.systemUserInvitationDenied("token_invalid", null);

            // La clave viaja con valor nulo, igual que en el gemelo del aprobador:
            // addKeyValue la escribe siempre. Lo que se fija aqui es que NO se
            // inventa un id, no que el campo desaparezca del evento.
            assertThat(fields()).containsEntry("event", "system_user_invitation_denied")
                    .containsEntry("reason", "token_invalid")
                    .containsEntry("system.user.request.id", null);
        }

        @Test
        @DisplayName("el alta del superadministrador sale en INFO y ata solicitud con cuenta creada")
        void el_alta_sale_en_info_y_ata_solicitud_con_cuenta() {
            logger.systemUserProvisioned(SOLICITUD, 9001L);

            // INFO deliberadamente: es un hecho normal de un flujo que funciono.
            // Subirlo a WARN «para que destaque» seria usar la severidad como
            // resaltador. Su visibilidad viene del contador y de la unica alerta.
            assertThat(emitted().getLevel()).isEqualTo(Level.INFO);
            assertThat(fields())
                    .isEqualTo(Map.of("event", "system_user_provisioned", "system.user.request.id",
                            SOLICITUD, "actor.systemUserId", 9001L, "outcome", "SUCCESS"));
        }
    }
}
