package com.vetsoftware.app.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.testsupport.AuthMother;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;

/**
 * Cierra sesión revocando refresh tokens e invalidando access tokens vivos. Lee
 * el principal del {@code SecurityContextHolder}: sin un actor de usuario real,
 * no hay nada que cerrar.
 */
@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AuthEmployeeRepository authEmployeeRepository;
    @Mock
    private AuthSystemUserRepository authSystemUserRepository;
    @InjectMocks
    private LogoutService service;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticar(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Nested
    @DisplayName("empleado autenticado")
    class Empleado {

        @Test
        @DisplayName("revoca todos sus refresh tokens y sube su authVersion")
        void revoca_refresh_tokens_y_sube_authVersion() {
            autenticar(AuthMother.empleado());

            service.execute();

            verify(refreshTokenRepository).revokeAllForSubject(AuthMother.EMPLOYEE_ID, "EMPLOYEE");
            verify(authEmployeeRepository).bumpAuthVersion(AuthMother.EMPLOYEE_ID,
                    AuthMother.COMPANY_ID);
            verifyNoInteractions(authSystemUserRepository);
        }
    }

    @Nested
    @DisplayName("usuario de sistema autenticado")
    class UsuarioDeSistema {

        @Test
        @DisplayName("revoca sus refresh tokens y sube su authVersion, sin tocar al empleado")
        void revoca_refresh_tokens_y_sube_authVersion() {
            autenticar(AuthMother.usuarioDeSistema());

            service.execute();

            verify(refreshTokenRepository).revokeAllForSubject(AuthMother.SYSTEM_USER_ID,
                    "SYSTEM_USER");
            verify(authSystemUserRepository).bumpAuthVersion(AuthMother.SYSTEM_USER_ID);
            verifyNoInteractions(authEmployeeRepository);
        }
    }

    @Nested
    @DisplayName("sin contexto de usuario")
    class SinContextoDeUsuario {

        @Test
        @DisplayName("el proceso interno de sistema no puede cerrar sesión: no escribe nada")
        void system_context_no_puede_cerrar_sesion() {
            autenticar(SystemContext.INSTANCE);

            assertThatThrownBy(service::execute).isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Not an authenticated user context");
            verifyNoInteractions(refreshTokenRepository, authEmployeeRepository,
                    authSystemUserRepository);
        }

        @Test
        @DisplayName("sin autenticación no escribe nada")
        void sin_autenticacion_no_escribe_nada() {
            assertThatThrownBy(service::execute).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(refreshTokenRepository, authEmployeeRepository,
                    authSystemUserRepository);
        }
    }
}
