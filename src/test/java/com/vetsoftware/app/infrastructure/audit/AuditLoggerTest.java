package com.vetsoftware.app.infrastructure.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.infrastructure.audit.outbox.AuditEventStore;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogger")
class AuditLoggerTest {

    @Mock
    private AuditEventStore eventStore;
    @InjectMocks
    private AuditLogger logger;

    @Captor
    private ArgumentCaptor<Map<String, Object>> attributesCaptor;

    @Nested
    @DisplayName("mutation")
    class Mutation {

        @Test
        @DisplayName("persiste metodo, ruta, estado y duracion de la mutacion HTTP")
        void persiste_metodo_ruta_estado_y_duracion() {
            logger.mutation("POST", "/api/v1/animals", 201, "SUCCESS", 42L);

            verify(eventStore).append(eq("http_mutation"), eq("SUCCESS"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(Map.of("http.method", "POST",
                    "http.path", "/api/v1/animals", "http.status", 201, "http.durationMs", 42L));
        }
    }

    @Nested
    @DisplayName("registro e invitaciones")
    class RegistroEInvitaciones {

        @Test
        @DisplayName("companyRegistered persiste los datos de la empresa y del dueño que la registro")
        void company_registered_persiste_los_datos_de_la_empresa() {
            logger.companyRegistered(1L, "Vet Uno", "900123456-7", 5L, "OWNER-01");

            verify(eventStore).append(eq("company_registered"), eq("SUCCESS"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(
                    Map.of("company.id", 1L, "company.name", "Vet Uno", "company.identifier",
                            "900123456-7", "actor.employeeId", 5L, "actor.identifier", "OWNER-01"));
        }

        @Test
        @DisplayName("employeeInvited persiste el empleado invitado y la empresa")
        void employee_invited_persiste_el_empleado_invitado() {
            logger.employeeInvited(9L, "EMP-09", 1L);

            verify(eventStore).append(eq("employee_invited"), eq("SUCCESS"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(
                    Map.of("employee.id", 9L, "employee.identifier", "EMP-09", "company.id", 1L));
        }

        @Test
        @DisplayName("employeeInvitationResent persiste el reenvio de la invitacion")
        void employee_invitation_resent_persiste_el_reenvio() {
            logger.employeeInvitationResent(9L, "EMP-09", 1L);

            verify(eventStore).append(eq("employee_invitation_resent"), eq("SUCCESS"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(
                    Map.of("employee.id", 9L, "employee.identifier", "EMP-09", "company.id", 1L));
        }

        @Test
        @DisplayName("invitationAccepted persiste el empleado que activo su cuenta")
        void invitation_accepted_persiste_el_empleado_activado() {
            logger.invitationAccepted(9L, 1L);

            verify(eventStore).append(eq("invitation_accepted"), eq("SUCCESS"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue())
                    .isEqualTo(Map.of("employee.id", 9L, "company.id", 1L));
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("loginSuccess persiste el tipo y el identificador del actor")
        void login_success_persiste_el_tipo_y_el_identificador() {
            logger.loginSuccess("EMPLOYEE", "EMP-09");

            verify(eventStore).append(eq("login_success"), eq("SUCCESS"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue())
                    .isEqualTo(Map.of("actor.type", "EMPLOYEE", "actor.identifier", "EMP-09"));
        }

        @Test
        @DisplayName("loginFailure persiste la ruta y el motivo del fallo")
        void login_failure_persiste_la_ruta_y_el_motivo() {
            logger.loginFailure("/api/v1/auth/login", "bad_credentials");

            verify(eventStore).append(eq("login_failure"), eq("FAILURE"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(
                    Map.of("http.path", "/api/v1/auth/login", "reason", "bad_credentials"));
        }

        @Test
        @DisplayName("loginBlockedEmailNotVerified persiste el identificador bloqueado")
        void login_blocked_email_not_verified_persiste_el_identificador() {
            logger.loginBlockedEmailNotVerified("EMP-09");

            verify(eventStore).append(eq("login_blocked_email_not_verified"), eq("DENIED"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(
                    Map.of("actor.identifier", "EMP-09", "reason", "email_not_verified"));
        }

        @Test
        @DisplayName("refreshTokenReuseDetected persiste el sujeto y el tiempo desde la revocacion")
        void refresh_token_reuse_detected_persiste_el_sujeto_y_el_tiempo() {
            logger.refreshTokenReuseDetected(9L, "EMPLOYEE", 30L);

            verify(eventStore).append(eq("refresh_token_reuse_detected"), eq("DENIED"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(Map.of("actor.id", "9", "actor.type",
                    "EMPLOYEE", "seconds_since_revocation", "30"));
        }
    }

    @Nested
    @DisplayName("autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("accessDenied persiste el metodo y la ruta denegados")
        void access_denied_persiste_metodo_y_ruta() {
            logger.accessDenied("DELETE", "/api/v1/animals/1");

            verify(eventStore).append(eq("access_denied"), eq("DENIED"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue())
                    .isEqualTo(Map.of("http.method", "DELETE", "http.path", "/api/v1/animals/1"));
        }

        @Test
        @DisplayName("unauthenticated persiste metodo, ruta y motivo")
        void unauthenticated_persiste_metodo_ruta_y_motivo() {
            logger.unauthenticated("GET", "/api/v1/animals", "missing_token");

            verify(eventStore).append(eq("unauthenticated"), eq("DENIED"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(Map.of("http.method", "GET",
                    "http.path", "/api/v1/animals", "reason", "missing_token"));
        }
    }

    @Nested
    @DisplayName("rate limiting")
    class RateLimiting {

        @Test
        @DisplayName("loginRateLimited persiste el codigo LOGIN_RATE_LIMITED")
        void login_rate_limited_persiste_el_codigo() {
            logger.loginRateLimited();

            verify(eventStore).append(eq("rate_limited"), eq("DENIED"), attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(Map.of("code", "LOGIN_RATE_LIMITED"));
        }

        @Test
        @DisplayName("rateLimited persiste el codigo recibido")
        void rate_limited_persiste_el_codigo_recibido() {
            logger.rateLimited("PUBLIC_ROUTE_RATE_LIMITED");

            verify(eventStore).append(eq("rate_limited"), eq("DENIED"), attributesCaptor.capture());
            assertThat(attributesCaptor.getValue())
                    .isEqualTo(Map.of("code", "PUBLIC_ROUTE_RATE_LIMITED"));
        }
    }
}
