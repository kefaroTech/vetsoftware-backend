package com.vetsoftware.app.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.ObjectMapper;

/**
 * Traduce los rechazos de la cadena de filtros al mismo {@code ProblemDetail}
 * que emite {@code AuthFilter}. El {@code ObjectMapper} es un colaborador de
 * datos, no un puerto: se instancia real, igual que en
 * {@code LoginRateLimitFilterTest}.
 */
@ExtendWith(MockitoExtension.class)
class SecurityProblemDetailHandlerTest {

    @Mock
    private AuditLogger auditLogger;
    @Mock
    private Tracer tracer;

    private SecurityProblemDetailHandler handler;

    @BeforeEach
    void construirHandler() {
        // new ObjectMapper() no es un puerto, es un colaborador de datos real; por
        // eso el handler se construye aquí y no en un inicializador de campo, que
        // correría antes de que MockitoExtension inyecte los @Mock.
        handler = new SecurityProblemDetailHandler(new ObjectMapper(), auditLogger, tracer);
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/animals");
        return request;
    }

    @Nested
    @DisplayName("commence — sin autenticación")
    class Commence {

        /**
         * Dos contratos distintos sobre el mismo rechazo, y por eso las dos aserciones:
         * al cliente sigue yendo la prosa {@code "Authentication
         * required"} —cambiarla rompería al front—, mientras que a la auditoría va el
         * código en snake_case {@code token_missing}, que es vocabulario cerrado y
         * agrupable en Grafana.
         */
        @Test
        @DisplayName("responde 401 con code TOKEN_MISSING y audita el motivo token_missing")
        void responde_401_y_audita() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthenticationException exception = org.mockito.Mockito
                    .mock(AuthenticationException.class);

            handler.commence(request(), response, exception);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).contains("TOKEN_MISSING")
                    .contains("Authentication required");
            verify(auditLogger).unauthenticated("GET", "/animals", "token_missing");
        }

        @Test
        @DisplayName("sin span activo no añade traceId")
        void sin_span_activo_no_anade_traceId() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(tracer.currentSpan()).thenReturn(null);

            handler.commence(request(), response,
                    org.mockito.Mockito.mock(AuthenticationException.class));

            assertThat(response.getContentAsString()).doesNotContain("traceId");
        }

        @Test
        @DisplayName("con un span activo añade el traceId al problem detail")
        void con_span_activo_anade_traceId() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            Span span = org.mockito.Mockito.mock(Span.class);
            TraceContext context = org.mockito.Mockito.mock(TraceContext.class);
            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(context);
            when(context.traceId()).thenReturn("trace-abc");

            handler.commence(request(), response,
                    org.mockito.Mockito.mock(AuthenticationException.class));

            assertThat(response.getContentAsString()).contains("trace-abc");
        }
    }

    @Nested
    @DisplayName("handle — sin autorización")
    class Handle {

        @Test
        @DisplayName("responde 403 con code FORBIDDEN y audita el rechazo")
        void responde_403_y_audita() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.handle(request(), response, new AccessDeniedException("no"));

            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getContentAsString()).contains("FORBIDDEN")
                    .contains("Access denied");
            verify(auditLogger).accessDenied("GET", "/animals");
        }
    }
}
