package com.vetsoftware.app.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.auth.application.port.out.BranchAccessResolver;
import com.vetsoftware.app.auth.application.port.out.EffectivePermissionResolver;
import com.vetsoftware.app.auth.application.port.out.PermissionResolver;
import com.vetsoftware.app.auth.testsupport.AuthMother;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reconstruye el {@code AuthContext} de un empleado en cada request a partir
 * del {@code employeeId} y la {@code authVersion} que trae el JWT. Es la pieza
 * que detecta una sesión reemplazada (BE-13): si esto deja pasar una versión
 * vieja, un token robado sigue sirviendo tras un logout forzado.
 */
@ExtendWith(MockitoExtension.class)
class ResolveAuthContextServiceTest {

    @Mock
    private PermissionResolver permissionResolver;
    @Mock
    private BranchAccessResolver branchAccessResolver;
    @Mock
    private EffectivePermissionResolver effectivePermissionResolver;
    @Mock
    private AuthEmployeeRepository employeeRepository;
    @InjectMocks
    private ResolveAuthContextService service;

    @Nested
    @DisplayName("Resolucion")
    class Resolucion {

        @Test
        @DisplayName("reconstruye el contexto con los permisos y sedes actuales, no los del token")
        void reconstruye_el_contexto_con_datos_actuales() {
            when(employeeRepository.findActiveById(AuthMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(AuthMother.sesionEmpleado()));
            when(permissionResolver.resolveFor(AuthMother.EMPLOYEE_ID))
                    .thenReturn(AuthMother.PERMISOS_EMPLEADO);
            Set<String> efectivos = Set.of("animal.read");
            when(effectivePermissionResolver.resolveFor(AuthMother.COMPANY_ID,
                    AuthMother.PERMISOS_EMPLEADO)).thenReturn(efectivos);
            when(branchAccessResolver.resolveFor(AuthMother.EMPLOYEE_ID))
                    .thenReturn(Set.of(AuthMother.BRANCH_ID));

            AuthContext result = service.execute(AuthMother.EMPLOYEE_ID, AuthMother.AUTH_VERSION);

            assertThat(result).isEqualTo(new EmployeeContext(AuthMother.EMPLOYEE_ID,
                    AuthMother.COMPANY_ID, efectivos, Set.of(AuthMother.BRANCH_ID)));
        }

        @Test
        @DisplayName("un employeeId nulo no consulta nada y devuelve null")
        void employee_id_nulo_no_consulta_nada() {
            assertThat(service.execute(null, AuthMother.AUTH_VERSION)).isNull();

            verifyNoInteractions(employeeRepository, permissionResolver,
                    effectivePermissionResolver, branchAccessResolver);
        }

        @Test
        @DisplayName("un empleado desconocido o desactivado devuelve null sin resolver permisos")
        void empleado_desconocido_devuelve_null() {
            when(employeeRepository.findActiveById(AuthMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThat(service.execute(AuthMother.EMPLOYEE_ID, AuthMother.AUTH_VERSION)).isNull();

            verifyNoInteractions(permissionResolver, effectivePermissionResolver,
                    branchAccessResolver);
        }
    }

    @Nested
    @DisplayName("Tenancy — versión de sesión")
    class VersionDeSesion {

        @Test
        @DisplayName("una authVersion distinta de la almacenada corta la sesión, sin resolver permisos")
        void authVersion_distinta_corta_la_sesion() {
            when(employeeRepository.findActiveById(AuthMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(AuthMother.sesionEmpleado()));

            assertThatThrownBy(() -> service.execute(AuthMother.EMPLOYEE_ID, 999L))
                    .isInstanceOf(SessionReplacedException.class);

            verifyNoInteractions(permissionResolver, effectivePermissionResolver,
                    branchAccessResolver);
        }
    }
}
