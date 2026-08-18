package com.vetsoftware.app.role.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.CompanyRef;
import com.vetsoftware.app.role.domain.Role;
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
@DisplayName("CreateRoleService")
class CreateRoleServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "NIT-900");

    @Mock
    private RoleRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private CreateRoleService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateRoleService(repository, companyQueryPort);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("guarda el rol con la empresa resuelta por el puerto, no otra")
        void guarda_el_rol_con_la_empresa_resuelta_por_el_puerto() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RoleDto dto = service.execute(new CreateRoleCommand("Veterinario", "VET", COMPANY_ID));

            ArgumentCaptor<Role> guardado = ArgumentCaptor.forClass(Role.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany()).isEqualTo(CLINICA);
            assertThat(guardado.getValue().getName()).isEqualTo("Veterinario");
            assertThat(guardado.getValue().getCode()).isEqualTo("VET");
            assertThat(dto.company().id()).isEqualTo(COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no persiste si la empresa del comando no existe")
        void no_persiste_si_la_empresa_no_existe() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new CreateRoleCommand("Veterinario", "VET", COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }
}
