package com.vetsoftware.app.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.dto.AuthSubjectType;
import com.vetsoftware.app.auth.application.dto.MeDto;
import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.application.port.out.EmployeeProfileQueryPort;
import com.vetsoftware.app.auth.application.port.out.SystemUserProfileQueryPort;
import com.vetsoftware.app.auth.testsupport.AuthMother;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

/**
 * {@code GetCurrentUserService} lee el principal ya resuelto por
 * {@code AuthFilter} desde el {@code SecurityContextHolder} — no recibe un id
 * por parámetro, así que un test que autentique el actor equivocado no lo
 * detectaría ningún compilador.
 */
@ExtendWith(MockitoExtension.class)
class GetCurrentUserServiceTest {

    @Mock
    private EmployeeProfileQueryPort employeeProfileQueryPort;
    @Mock
    private SystemUserProfileQueryPort systemUserProfileQueryPort;
    @InjectMocks
    private GetCurrentUserService service;

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
        @DisplayName("compone el MeDto con el perfil resuelto y los permisos ya presentes en el contexto")
        void compone_el_medto_con_el_perfil_resuelto() {
            autenticar(AuthMother.empleado());
            when(employeeProfileQueryPort.findById(AuthMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(AuthMother.perfilEmpleado()));

            MeDto result = service.execute();

            assertThat(result).isEqualTo(new MeDto(AuthMother.EMPLOYEE_ID, AuthSubjectType.EMPLOYEE,
                    AuthMother.COMPANY_ID, "Ana Ruiz", "EMP-1", false, AuthMother.PERMISOS_EMPLEADO,
                    Set.of(AuthMother.BRANCH_ID)));
            verifyNoInteractions(systemUserProfileQueryPort);
        }

        @Test
        @DisplayName("un perfil de empleado que ya no existe deniega el acceso")
        void perfil_inexistente_deniega_el_acceso() {
            autenticar(AuthMother.empleado());
            when(employeeProfileQueryPort.findById(AuthMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(service::execute).isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Employee profile not found");
        }
    }

    @Nested
    @DisplayName("usuario de sistema autenticado")
    class UsuarioDeSistema {

        @Test
        @DisplayName("compone el MeDto sin empresa ni sedes: son conceptos de empleado")
        void compone_el_medto_sin_empresa_ni_sedes() {
            autenticar(AuthMother.usuarioDeSistema());
            when(systemUserProfileQueryPort.findById(AuthMother.SYSTEM_USER_ID))
                    .thenReturn(Optional.of(AuthMother.perfilSistema()));

            MeDto result = service.execute();

            assertThat(result).isEqualTo(new MeDto(AuthMother.SYSTEM_USER_ID,
                    AuthSubjectType.SYSTEM_USER, null, "ADMIN", null, false, Set.of(), Set.of()));
            verifyNoInteractions(employeeProfileQueryPort);
        }

        @Test
        @DisplayName("un perfil de usuario de sistema que ya no existe deniega el acceso")
        void perfil_inexistente_deniega_el_acceso() {
            autenticar(AuthMother.usuarioDeSistema());
            when(systemUserProfileQueryPort.findById(AuthMother.SYSTEM_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(service::execute).isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("System user profile not found");
        }
    }

    @Nested
    @DisplayName("sin contexto de usuario")
    class SinContextoDeUsuario {

        @Test
        @DisplayName("el proceso interno de sistema no es un usuario: se deniega")
        void system_context_se_deniega() {
            autenticar(SystemContext.INSTANCE);

            assertThatThrownBy(service::execute).isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Not an authenticated user context");
            verifyNoInteractions(employeeProfileQueryPort, systemUserProfileQueryPort);
        }

        @Test
        @DisplayName("sin autenticación se deniega")
        void sin_autenticacion_se_deniega() {
            assertThatThrownBy(service::execute).isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Not an authenticated user context");
        }
    }
}
