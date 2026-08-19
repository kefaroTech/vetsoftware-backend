package com.vetsoftware.app.role.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.application.command.UpdateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.CompanyRef;
import com.vetsoftware.app.role.domain.Role;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateRoleService")
class UpdateRoleServiceTest {

    private static final Long ROLE_ID = 1L;
    private static final Long COMPANY_ID = 9L;
    private static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "NIT-900");

    @Mock
    private RoleRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private UpdateRoleService service;

    @BeforeEach
    void crearServicio() {
        service = new UpdateRoleService(repository, companyQueryPort);
    }

    private static Role roleExistente() {
        return new Role(ROLE_ID, "Veterinario", "VET", CLINICA,
                LocalDateTime.of(2026, 1, 15, 10, 30), null, true);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el rol encontrado dentro de la empresa autenticada y lo guarda")
        void actualiza_el_rol_encontrado_dentro_de_la_empresa() {
            when(repository.findByIdAndCompanyId(ROLE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(roleExistente()));
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RoleDto dto = service
                    .execute(new UpdateRoleCommand(ROLE_ID, "Administrador", "ADMIN", COMPANY_ID));

            ArgumentCaptor<Role> guardado = ArgumentCaptor.forClass(Role.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getName()).isEqualTo("Administrador");
            assertThat(guardado.getValue().getCode()).isEqualTo("ADMIN");
            assertThat(dto.name()).isEqualTo("Administrador");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no guarda si la empresa del comando no existe")
        void no_guarda_si_la_empresa_no_existe() {
            when(repository.findByIdAndCompanyId(ROLE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(roleExistente()));
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new UpdateRoleCommand(ROLE_ID, "Administrador", "ADMIN", COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("tenancy")
    class Tenancy {

        @Test
        @DisplayName("un rol de otra empresa no se resuelve: la busqueda ya viene acotada por companyId")
        void un_rol_de_otra_empresa_no_se_resuelve() {
            Long otraCompanyId = 77L;
            when(repository.findByIdAndCompanyId(ROLE_ID, otraCompanyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    new UpdateRoleCommand(ROLE_ID, "Administrador", "ADMIN", otraCompanyId)))
                    .isInstanceOf(RoleNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ROLE_ID));

            verify(repository).findByIdAndCompanyId(ROLE_ID, otraCompanyId);
            verifyNoMoreInteractions(repository, companyQueryPort);
        }
    }
}
