package com.vetsoftware.app.role.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import com.vetsoftware.app.role.testsupport.RoleMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateRoleService")
class ReactivateRoleServiceTest {

    private static final Long ROLE_ID = RoleMother.ROLE_ID;
    private static final Long COMPANY_ID = RoleMother.COMPANY_ID;

    @Mock
    private RoleRepository repository;

    private ReactivateRoleService service;

    @BeforeEach
    void crearServicio() {
        service = new ReactivateRoleService(repository);
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("con filas afectadas, devuelve el rol releido dentro de la empresa")
        void con_filas_afectadas_devuelve_el_rol_releido() {
            when(repository.reactivate(ROLE_ID, COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ROLE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(RoleMother.veterinario()));

            RoleDto dto = service.execute(ROLE_ID, COMPANY_ID);

            assertThat(dto.id()).isEqualTo(ROLE_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin filas afectadas, el update ya vino acotado por empresa y no reactiva nada")
        void sin_filas_afectadas_no_reactiva_nada() {
            when(repository.reactivate(ROLE_ID, COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ROLE_ID, COMPANY_ID))
                    .isInstanceOf(RoleNotFoundException.class);

            verify(repository).reactivate(ROLE_ID, COMPANY_ID);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("filas afectadas pero sin relectura posterior tambien es not-found")
        void filas_afectadas_pero_sin_relectura_es_not_found() {
            when(repository.reactivate(ROLE_ID, COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ROLE_ID, COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ROLE_ID, COMPANY_ID))
                    .isInstanceOf(RoleNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("tenancy")
    class Tenancy {

        @Test
        @DisplayName("reactivate se llama siempre con la empresa autenticada, no con otra")
        void reactivate_se_llama_con_la_empresa_autenticada() {
            Long otraCompanyId = 77L;
            when(repository.reactivate(ROLE_ID, otraCompanyId)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ROLE_ID, otraCompanyId))
                    .isInstanceOf(RoleNotFoundException.class);

            verify(repository).reactivate(ROLE_ID, otraCompanyId);
            verifyNoMoreInteractions(repository);
        }
    }
}
