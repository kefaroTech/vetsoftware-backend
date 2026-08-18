package com.vetsoftware.app.employeerole.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeerole.domain.RoleRef;
import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaRoleQueryPort (employeerole) — resuelve el RoleRef desde el repositorio Spring Data")
class JpaRoleQueryPortTest {

    @Mock
    private RoleJpaRepository roleJpaRepository;

    @InjectMocks
    private JpaRoleQueryPort port;

    private static RoleJpaEntity entidad(Long id, String name, String code) {
        RoleJpaEntity e = mock(RoleJpaEntity.class);
        when(e.getId()).thenReturn(id);
        when(e.getName()).thenReturn(name);
        when(e.getCode()).thenReturn(code);
        return e;
    }

    @Test
    @DisplayName("mapea el rol encontrado al RoleRef")
    void mapea_el_rol_encontrado() {
        RoleJpaEntity entity = entidad(EmployeeRoleMother.ROL_VETERINARIO.id(),
                EmployeeRoleMother.ROL_VETERINARIO.name(),
                EmployeeRoleMother.ROL_VETERINARIO.code());
        when(roleJpaRepository.findById(EmployeeRoleMother.ROL_VETERINARIO.id()))
                .thenReturn(Optional.of(entity));

        Optional<RoleRef> result = port.findById(EmployeeRoleMother.ROL_VETERINARIO.id());

        assertThat(result).contains(EmployeeRoleMother.ROL_VETERINARIO);
    }

    @Test
    @DisplayName("vacio si el rol no existe")
    void vacio_si_el_rol_no_existe() {
        when(roleJpaRepository.findById(EmployeeRoleMother.ROL_VETERINARIO.id()))
                .thenReturn(Optional.empty());

        assertThat(port.findById(EmployeeRoleMother.ROL_VETERINARIO.id())).isEmpty();
    }

    @Test
    @DisplayName("la variante acotada consulta por id Y empresa, no solo por id")
    void la_variante_acotada_consulta_por_id_y_empresa() {
        RoleRef rol = EmployeeRoleMother.ROL_VETERINARIO;
        RoleJpaEntity entity = entidad(rol.id(), rol.name(), rol.code());
        when(roleJpaRepository.findByIdAndCompany_Id(rol.id(), EmployeeRoleMother.EMPRESA))
                .thenReturn(Optional.of(entity));

        Optional<RoleRef> result = port.findByIdAndCompanyId(rol.id(), EmployeeRoleMother.EMPRESA);

        assertThat(result).contains(rol);
    }

    /**
     * Los roles tienen {@code company_id}: el rol de otra empresa sale vacio y no
     * se puede colgar de un empleado propio.
     */
    @Test
    @DisplayName("vacio si el rol es de otra empresa")
    void vacio_si_el_rol_es_de_otra_empresa() {
        when(roleJpaRepository.findByIdAndCompany_Id(EmployeeRoleMother.ROL_VETERINARIO.id(),
                EmployeeRoleMother.EMPRESA)).thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(EmployeeRoleMother.ROL_VETERINARIO.id(),
                EmployeeRoleMother.EMPRESA)).isEmpty();
    }
}
