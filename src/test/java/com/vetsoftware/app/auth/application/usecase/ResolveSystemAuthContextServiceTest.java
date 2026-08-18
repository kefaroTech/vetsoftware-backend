package com.vetsoftware.app.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository;
import com.vetsoftware.app.auth.application.port.out.SystemPermissionResolver;
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

@ExtendWith(MockitoExtension.class)
class ResolveSystemAuthContextServiceTest {

    @Mock
    private SystemPermissionResolver permissionResolver;
    @Mock
    private AuthSystemUserRepository systemUserRepository;
    @InjectMocks
    private ResolveSystemAuthContextService service;

    @Nested
    @DisplayName("Resolucion")
    class Resolucion {

        @Test
        @DisplayName("reconstruye el contexto del usuario de sistema con sus permisos actuales")
        void reconstruye_el_contexto_con_permisos_actuales() {
            when(systemUserRepository.findActiveById(AuthMother.SYSTEM_USER_ID))
                    .thenReturn(Optional.of(AuthMother.sesionSistema()));
            when(permissionResolver.resolveFor(AuthMother.SYSTEM_USER_ID))
                    .thenReturn(Set.of("company.create"));

            AuthContext result = service.execute(AuthMother.SYSTEM_USER_ID,
                    AuthMother.AUTH_VERSION);

            assertThat(result).isEqualTo(
                    new SystemUserContext(AuthMother.SYSTEM_USER_ID, Set.of("company.create")));
        }

        @Test
        @DisplayName("un systemUserId nulo no consulta nada y devuelve null")
        void system_user_id_nulo_no_consulta_nada() {
            assertThat(service.execute(null, AuthMother.AUTH_VERSION)).isNull();

            verifyNoInteractions(systemUserRepository, permissionResolver);
        }

        @Test
        @DisplayName("un usuario de sistema desconocido devuelve null sin resolver permisos")
        void usuario_desconocido_devuelve_null() {
            when(systemUserRepository.findActiveById(AuthMother.SYSTEM_USER_ID))
                    .thenReturn(Optional.empty());

            assertThat(service.execute(AuthMother.SYSTEM_USER_ID, AuthMother.AUTH_VERSION))
                    .isNull();

            verifyNoInteractions(permissionResolver);
        }
    }

    @Nested
    @DisplayName("Tenancy — versión de sesión")
    class VersionDeSesion {

        @Test
        @DisplayName("una authVersion distinta de la almacenada corta la sesión")
        void authVersion_distinta_corta_la_sesion() {
            when(systemUserRepository.findActiveById(AuthMother.SYSTEM_USER_ID))
                    .thenReturn(Optional.of(AuthMother.sesionSistema()));

            assertThatThrownBy(() -> service.execute(AuthMother.SYSTEM_USER_ID, 999L))
                    .isInstanceOf(SessionReplacedException.class);

            verifyNoInteractions(permissionResolver);
        }
    }
}
