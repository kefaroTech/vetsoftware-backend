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

        @Test
        @DisplayName("unauthenticated emite el motivo y deja metodo y ruta en el mensaje")
        void unauthenticated_emite_motivo() {
            logger.unauthenticated("GET", "/api/v1/animals", "missing_token");

            assertThat(emitted().getLevel()).isEqualTo(Level.WARN);
            assertThat(fields()).isEqualTo(Map.of("event", "unauthenticated", "outcome", "DENIED",
                    "reason", "missing_token"));
            assertThat(emitted().getFormattedMessage())
                    .isEqualTo("unauthenticated GET /api/v1/animals reason=missing_token");
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
}
