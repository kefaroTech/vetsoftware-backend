package com.vetsoftware.app.auth.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.in.ResolveSystemAuthContextUseCase;
import com.vetsoftware.app.auth.infrastructure.security.JwtProvider;
import com.vetsoftware.app.auth.testsupport.AuthMother;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

/**
 * Valida el JWT de cada request y deja el {@code AuthContext} en el
 * {@code SecurityContextHolder}. Es el punto donde vivió BE-01: un tipo de
 * token o un contexto mal resuelto aquí anula toda la autorización posterior.
 */
@ExtendWith(MockitoExtension.class)
class AuthFilterTest {

    @Mock
    private ResolveAuthContextUseCase resolveAuthContextUseCase;
    @Mock
    private ResolveSystemAuthContextUseCase resolveSystemAuthContextUseCase;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private Tracer tracer;
    @Mock
    private FilterChain filterChain;

    private AuthFilter filter;

    @BeforeEach
    void construirFiltro() {
        // Se construye aquí, no en un inicializador de campo: éste correría antes de
        // que MockitoExtension inyecte los @Mock y el filtro quedaría con null.
        filter = new AuthFilter(resolveAuthContextUseCase, resolveSystemAuthContextUseCase,
                jwtProvider, new ObjectMapper(), auditLogger, tracer);
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private static MockHttpServletRequest requestConToken(String token) {
        MockHttpServletRequest request = request("GET", "/animals");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class RutasExcluidas {

        @Test
        @DisplayName("OPTIONS nunca se filtra, sea cual sea la ruta")
        void options_nunca_se_filtra() {
            assertThat(filter.shouldNotFilter(request("OPTIONS", "/animals"))).isTrue();
        }

        @Test
        @DisplayName("una ruta pública de login no exige JWT")
        void ruta_publica_no_exige_jwt() {
            assertThat(filter.shouldNotFilter(request("POST", "/auth/login/employee"))).isTrue();
        }

        @Test
        @DisplayName("una ruta de negocio exige JWT")
        void ruta_de_negocio_exige_jwt() {
            assertThat(filter.shouldNotFilter(request("GET", "/animals"))).isFalse();
        }
    }

    @Nested
    @DisplayName("rechazo antes de resolver el contexto")
    class RechazoTemprano {

        @Test
        @DisplayName("sin cabecera Authorization responde 401 TOKEN_MISSING y no llega a la cadena")
        void sin_cabecera_responde_401() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request("GET", "/animals"), response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).contains("TOKEN_MISSING");
            verifyNoInteractions(filterChain);
            verify(auditLogger).unauthenticated("GET", "/animals",
                    "Missing or invalid Authorization header");
        }

        @Test
        @DisplayName("una cabecera que no empieza por 'Bearer ' responde 401 TOKEN_MISSING")
        void cabecera_sin_bearer_responde_401() throws Exception {
            MockHttpServletRequest request = request("GET", "/animals");
            request.addHeader("Authorization", "Basic xyz");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("un token vencido responde 401 TOKEN_EXPIRED, distinto de uno inválido")
        void token_vencido_responde_401_expired() throws Exception {
            when(jwtProvider.extractType(anyString())).thenThrow(mock(ExpiredJwtException.class));
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(requestConToken("vencido"), response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).contains("TOKEN_EXPIRED");
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("un token corrupto responde 401 TOKEN_INVALID")
        void token_corrupto_responde_401_invalid() throws Exception {
            when(jwtProvider.extractType(anyString()))
                    .thenThrow(new JwtException("firma inválida"));
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(requestConToken("corrupto"), response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).contains("TOKEN_INVALID");
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("un id ausente en el token responde 401 TOKEN_INVALID sin resolver contexto")
        void id_ausente_responde_401_invalid() throws Exception {
            when(jwtProvider.extractType(anyString())).thenReturn("EMPLOYEE");
            when(jwtProvider.extractId(anyString())).thenReturn(null);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(requestConToken("sin-id"), response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verifyNoInteractions(resolveAuthContextUseCase);
            verifyNoInteractions(filterChain);
        }

        @ParameterizedTest
        @ValueSource(strings = {"REFRESH_TOKEN", "OTHER", ""})
        @DisplayName("un tipo de sujeto desconocido no resuelve ningún contexto y responde 401")
        void tipo_desconocido_no_resuelve_contexto(String tipo) throws Exception {
            when(jwtProvider.extractType(anyString())).thenReturn(tipo);
            when(jwtProvider.extractId(anyString())).thenReturn(7L);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(requestConToken("tipo-raro"), response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).contains("Unknown or revoked user");
            verifyNoInteractions(resolveAuthContextUseCase);
            verifyNoInteractions(resolveSystemAuthContextUseCase);
            verifyNoInteractions(filterChain);
        }
    }

    @Nested
    @DisplayName("resolución del contexto de empleado")
    class ContextoDeEmpleado {

        @Test
        @DisplayName("una sesión reemplazada responde 401 SESSION_REPLACED")
        void sesion_reemplazada_responde_401() throws Exception {
            when(jwtProvider.extractType(anyString())).thenReturn("EMPLOYEE");
            when(jwtProvider.extractId(anyString())).thenReturn(AuthMother.EMPLOYEE_ID);
            when(jwtProvider.extractAuthVersion(anyString())).thenReturn(AuthMother.AUTH_VERSION);
            when(resolveAuthContextUseCase.execute(AuthMother.EMPLOYEE_ID, AuthMother.AUTH_VERSION))
                    .thenThrow(new SessionReplacedException());
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(requestConToken("reemplazado"), response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).contains("SESSION_REPLACED");
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("un empleado desconocido o revocado responde 401 TOKEN_INVALID")
        void empleado_desconocido_responde_401() throws Exception {
            when(jwtProvider.extractType(anyString())).thenReturn("EMPLOYEE");
            when(jwtProvider.extractId(anyString())).thenReturn(AuthMother.EMPLOYEE_ID);
            when(resolveAuthContextUseCase.execute(eq(AuthMother.EMPLOYEE_ID),
                    org.mockito.ArgumentMatchers.any())).thenReturn(null);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(requestConToken("desconocido"), response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("un empleado válido llega a la cadena autenticado con sus permisos como autoridades")
        void empleado_valido_llega_autenticado() throws Exception {
            EmployeeContext contexto = AuthMother.empleado();
            when(jwtProvider.extractType(anyString())).thenReturn("EMPLOYEE");
            when(jwtProvider.extractId(anyString())).thenReturn(AuthMother.EMPLOYEE_ID);
            when(jwtProvider.extractAuthVersion(anyString())).thenReturn(AuthMother.AUTH_VERSION);
            when(resolveAuthContextUseCase.execute(AuthMother.EMPLOYEE_ID, AuthMother.AUTH_VERSION))
                    .thenReturn(contexto);
            MockHttpServletResponse response = new MockHttpServletResponse();
            Set<GrantedAuthority>[] autoridades = new Set[1];
            String[] mdcActorType = new String[1];
            doAnswer(inv -> {
                autoridades[0] = Set.copyOf(
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                mdcActorType[0] = MDC.get(MdcKeys.ACTOR_TYPE);
                return null;
            }).when(filterChain).doFilter(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());

            filter.doFilterInternal(requestConToken("valido"), response, filterChain);

            verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
            assertThat(mdcActorType[0]).isEqualTo("EMPLOYEE");
            assertThat(autoridades[0]).extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrderElementsOf(AuthMother.PERMISOS_EMPLEADO);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(MDC.get(MdcKeys.ACTOR_TYPE)).isNull();
        }
    }

    @Nested
    @DisplayName("resolución del contexto de usuario de sistema")
    class ContextoDeSistema {

        @Test
        @DisplayName("un usuario de sistema válido llega a la cadena con ROLE_SYSTEM")
        void usuario_de_sistema_valido_llega_con_role_system() throws Exception {
            SystemUserContext contexto = AuthMother.usuarioDeSistema();
            when(jwtProvider.extractType(anyString())).thenReturn("SYSTEM_USER");
            when(jwtProvider.extractId(anyString())).thenReturn(AuthMother.SYSTEM_USER_ID);
            when(jwtProvider.extractAuthVersion(anyString())).thenReturn(AuthMother.AUTH_VERSION);
            when(resolveSystemAuthContextUseCase.execute(AuthMother.SYSTEM_USER_ID,
                    AuthMother.AUTH_VERSION)).thenReturn(contexto);
            MockHttpServletResponse response = new MockHttpServletResponse();
            Set<GrantedAuthority>[] autoridades = new Set[1];
            doAnswer(inv -> {
                autoridades[0] = Set.copyOf(
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                return null;
            }).when(filterChain).doFilter(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());

            filter.doFilterInternal(requestConToken("sistema"), response, filterChain);

            assertThat(autoridades[0]).extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_SYSTEM");
            verifyNoInteractions(resolveAuthContextUseCase);
        }

        @Test
        @DisplayName("una sesión de sistema reemplazada responde 401 SESSION_REPLACED")
        void sesion_de_sistema_reemplazada_responde_401() throws Exception {
            when(jwtProvider.extractType(anyString())).thenReturn("SYSTEM_USER");
            when(jwtProvider.extractId(anyString())).thenReturn(AuthMother.SYSTEM_USER_ID);
            when(jwtProvider.extractAuthVersion(anyString())).thenReturn(AuthMother.AUTH_VERSION);
            when(resolveSystemAuthContextUseCase.execute(AuthMother.SYSTEM_USER_ID,
                    AuthMother.AUTH_VERSION)).thenThrow(new SessionReplacedException());
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(requestConToken("reemplazado"), response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(resolveAuthContextUseCase, never()).execute(
                    org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("un contexto de sistema puro llega a la cadena con ROLE_SYSTEM y sin permisos propios")
        void contexto_de_sistema_puro_llega_con_role_system() throws Exception {
            // AuthContext es una jerarquía sellada de tres implementadores, pero solo
            // dos nacen hoy de un puerto real (EmployeeContext, SystemUserContext).
            // SystemContext es el tercero: sin este caso, los dos switch exhaustivos
            // de AuthFilter (putActorToMdc y toAuthentication) nunca ejercitan esa rama.
            when(jwtProvider.extractType(anyString())).thenReturn("SYSTEM_USER");
            when(jwtProvider.extractId(anyString())).thenReturn(AuthMother.SYSTEM_USER_ID);
            when(jwtProvider.extractAuthVersion(anyString())).thenReturn(AuthMother.AUTH_VERSION);
            when(resolveSystemAuthContextUseCase.execute(AuthMother.SYSTEM_USER_ID,
                    AuthMother.AUTH_VERSION)).thenReturn(SystemContext.INSTANCE);
            MockHttpServletResponse response = new MockHttpServletResponse();
            Set<GrantedAuthority>[] autoridades = new Set[1];
            String[] mdcActorType = new String[1];
            doAnswer(inv -> {
                autoridades[0] = Set.copyOf(
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                mdcActorType[0] = MDC.get(MdcKeys.ACTOR_TYPE);
                return null;
            }).when(filterChain).doFilter(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());

            filter.doFilterInternal(requestConToken("sistema-puro"), response, filterChain);

            assertThat(mdcActorType[0]).isEqualTo("SYSTEM");
            assertThat(autoridades[0]).extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_SYSTEM");
            verifyNoInteractions(resolveAuthContextUseCase);
        }
    }
}
