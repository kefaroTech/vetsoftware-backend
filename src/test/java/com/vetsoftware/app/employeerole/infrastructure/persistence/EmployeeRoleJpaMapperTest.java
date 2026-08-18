package com.vetsoftware.app.employeerole.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Las entidades JPA de empleado y rol se mockean porque su constructor sin
 * argumentos es {@code protected}: no son instanciables desde este paquete y no
 * tienen logica propia que ocultar, igual que en {@code AnimalJpaMapperTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeRoleJpaMapper")
class EmployeeRoleJpaMapperTest {

    private final EmployeeRoleJpaMapper mapper = new EmployeeRoleJpaMapper();

    @Mock
    private EmployeeJpaEntity employeeEntity;
    @Mock
    private RoleJpaEntity roleEntity;

    private EmployeeRoleJpaEntity entidadCompleta() {
        EmployeeRoleJpaEntity entity = new EmployeeRoleJpaEntity();
        entity.setId(EmployeeRoleMother.EMPLOYEE_ROLE_ID);
        entity.setCreatedDate(EmployeeRoleMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar y engancha cada asociacion en su slot")
        void copia_cada_campo_y_engancha_las_asociaciones() {
            EmployeeRole employeeRole = EmployeeRoleMother.habilitado();

            EmployeeRoleJpaEntity entity = mapper.toJpa(employeeRole, employeeEntity, roleEntity);

            assertThat(entity.getId()).isEqualTo(EmployeeRoleMother.EMPLOYEE_ROLE_ID);
            assertThat(entity.getCreatedDate()).isEqualTo(EmployeeRoleMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.getEmployee()).isSameAs(employeeEntity);
            assertThat(entity.getRole()).isSameAs(roleEntity);
        }

        @Test
        @DisplayName("conserva el estado deshabilitado")
        void conserva_el_estado_deshabilitado() {
            EmployeeRole employeeRole = EmployeeRoleMother.deshabilitado();

            EmployeeRoleJpaEntity entity = mapper.toJpa(employeeRole, employeeEntity, roleEntity);

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar los proxies lazy de employee/role:
            // si leyera entity.getEmployee(), Hibernate lanzaria un SELECT extra por save.
            EmployeeRole employeeRole = mapper.toDomain(entidadCompleta(),
                    EmployeeRoleMother.EMPLEADO, EmployeeRoleMother.ROL_VETERINARIO);

            assertThat(employeeRole.getId()).isEqualTo(EmployeeRoleMother.EMPLOYEE_ROLE_ID);
            assertThat(employeeRole.getEmployee()).isEqualTo(EmployeeRoleMother.EMPLEADO);
            assertThat(employeeRole.getRole()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO);
            assertThat(employeeRole.getCreatedDate()).isEqualTo(EmployeeRoleMother.CREADO);
            assertThat(employeeRole.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            EmployeeRole original = EmployeeRoleMother.habilitado();

            EmployeeRoleJpaEntity entity = mapper.toJpa(original, employeeEntity, roleEntity);
            EmployeeRole vuelta = mapper.toDomain(entity, original.getEmployee(),
                    original.getRole());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(employeeEntity.getId()).thenReturn(EmployeeRoleMother.EMPLEADO.id());
            when(employeeEntity.getEmployeeCode())
                    .thenReturn(EmployeeRoleMother.EMPLEADO.employeeCode());
            when(employeeEntity.getName()).thenReturn(EmployeeRoleMother.EMPLEADO.name());
            when(roleEntity.getId()).thenReturn(EmployeeRoleMother.ROL_VETERINARIO.id());
            when(roleEntity.getName()).thenReturn(EmployeeRoleMother.ROL_VETERINARIO.name());
            when(roleEntity.getCode()).thenReturn(EmployeeRoleMother.ROL_VETERINARIO.code());

            EmployeeRoleJpaEntity entity = entidadCompleta();
            entity.setEmployee(employeeEntity);
            entity.setRole(roleEntity);

            EmployeeRole employeeRole = mapper.toDomain(entity);

            assertThat(employeeRole.getEmployee()).isEqualTo(EmployeeRoleMother.EMPLEADO);
            assertThat(employeeRole.getRole()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO);
        }
    }
}
