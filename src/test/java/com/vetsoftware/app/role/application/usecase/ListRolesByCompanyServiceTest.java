package com.vetsoftware.app.role.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.application.dto.PermissionSummaryDto;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.out.RolePermissionsForRolesQueryPort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.Role;
import com.vetsoftware.app.role.testsupport.RoleMother;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListRolesByCompanyService")
class ListRolesByCompanyServiceTest {

    private static final Long COMPANY_ID = RoleMother.COMPANY_ID;

    @Mock
    private RoleRepository repository;
    @Mock
    private RolePermissionsForRolesQueryPort permissionsPort;

    private ListRolesByCompanyService service;

    @BeforeEach
    void crearServicio() {
        service = new ListRolesByCompanyService(repository, permissionsPort);
    }

    @Nested
    @DisplayName("listado por empresa")
    class ListadoPorEmpresa {

        @Test
        @DisplayName("una empresa sin roles no consulta los permisos")
        void una_empresa_sin_roles_no_consulta_los_permisos() {
            when(repository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());

            List<RoleDto> resultado = service.listByCompany(COMPANY_ID);

            assertThat(resultado).isEmpty();
            verifyNoInteractions(permissionsPort);
        }

        @Test
        @DisplayName("adjunta los permisos de cada rol resueltos en una sola consulta")
        void adjunta_los_permisos_de_cada_rol() {
            Role veterinario = RoleMother.veterinario();
            Role administrador = RoleMother.administrador();
            when(repository.findAllByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(veterinario, administrador));
            List<PermissionSummaryDto> permisosVet = List
                    .of(new PermissionSummaryDto(1L, 2L, "Ver animales", "ANIMAL_READ"));
            when(permissionsPort.findByRoleIds(List.of(veterinario.getId(), administrador.getId())))
                    .thenReturn(Map.of(veterinario.getId(), permisosVet));

            List<RoleDto> resultado = service.listByCompany(COMPANY_ID);

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).permissions()).isEqualTo(permisosVet);
            assertThat(resultado.get(1).permissions()).isEmpty();
        }
    }
}
