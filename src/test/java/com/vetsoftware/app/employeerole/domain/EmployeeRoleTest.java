package com.vetsoftware.app.employeerole.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeRole")
class EmployeeRoleTest {

    @Nested
    @DisplayName("constructor")
    class Construccion {

        @Test
        @DisplayName("exige un empleado")
        void exige_un_empleado() {
            assertThatThrownBy(() -> new EmployeeRole(1L, null, EmployeeRoleMother.ROL_VETERINARIO,
                    EmployeeRoleMother.CREADO, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee is required");
        }

        @Test
        @DisplayName("exige un rol")
        void exige_un_rol() {
            assertThatThrownBy(() -> new EmployeeRole(1L, EmployeeRoleMother.EMPLEADO, null,
                    EmployeeRoleMother.CREADO, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role is required");
        }

        @Test
        @DisplayName("con datos validos, expone cada campo tal cual se paso")
        void con_datos_validos_expone_cada_campo() {
            EmployeeRole employeeRole = new EmployeeRole(500L, EmployeeRoleMother.EMPLEADO,
                    EmployeeRoleMother.ROL_VETERINARIO, EmployeeRoleMother.CREADO, true);

            assertThat(employeeRole.getId()).isEqualTo(500L);
            assertThat(employeeRole.getEmployee()).isEqualTo(EmployeeRoleMother.EMPLEADO);
            assertThat(employeeRole.getRole()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO);
            assertThat(employeeRole.getCreatedDate()).isEqualTo(EmployeeRoleMother.CREADO);
            assertThat(employeeRole.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("create — factory de alta")
    class Creacion {

        @Test
        @DisplayName("nace sin id, habilitada y con la fecha de creacion resuelta")
        void nace_sin_id_habilitada_y_con_fecha() {
            EmployeeRole employeeRole = EmployeeRole.create(EmployeeRoleMother.EMPLEADO,
                    EmployeeRoleMother.ROL_VETERINARIO);

            assertThat(employeeRole.getId()).isNull();
            assertThat(employeeRole.getEmployee()).isEqualTo(EmployeeRoleMother.EMPLEADO);
            assertThat(employeeRole.getRole()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO);
            assertThat(employeeRole.isEnabled()).isTrue();
            // createdDate viene de LocalDateTime.now(): no es afirmable, solo que exista.
            assertThat(employeeRole.getCreatedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("update — reasignacion de empleado y rol")
    class Actualizacion {

        @Test
        @DisplayName("reemplaza el empleado y el rol conservando el resto")
        void reemplaza_empleado_y_rol() {
            EmployeeRole employeeRole = EmployeeRoleMother.habilitado();

            employeeRole.update(EmployeeRoleMother.OTRO_EMPLEADO, EmployeeRoleMother.ROL_RECEPCION);

            assertThat(employeeRole.getEmployee()).isEqualTo(EmployeeRoleMother.OTRO_EMPLEADO);
            assertThat(employeeRole.getRole()).isEqualTo(EmployeeRoleMother.ROL_RECEPCION);
            assertThat(employeeRole.getId()).isEqualTo(EmployeeRoleMother.EMPLOYEE_ROLE_ID);
            assertThat(employeeRole.getCreatedDate()).isEqualTo(EmployeeRoleMother.CREADO);
        }

        @Test
        @DisplayName("rechaza un empleado nulo sin tocar el estado previo")
        void rechaza_empleado_nulo() {
            EmployeeRole employeeRole = EmployeeRoleMother.habilitado();

            assertThatThrownBy(() -> employeeRole.update(null, EmployeeRoleMother.ROL_RECEPCION))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee is required");
            assertThat(employeeRole.getEmployee()).isEqualTo(EmployeeRoleMother.EMPLEADO);
            assertThat(employeeRole.getRole()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO);
        }

        @Test
        @DisplayName("rechaza un rol nulo sin tocar el estado previo")
        void rechaza_rol_nulo() {
            EmployeeRole employeeRole = EmployeeRoleMother.habilitado();

            assertThatThrownBy(() -> employeeRole.update(EmployeeRoleMother.OTRO_EMPLEADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role is required");
            assertThat(employeeRole.getEmployee()).isEqualTo(EmployeeRoleMother.EMPLEADO);
            assertThat(employeeRole.getRole()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO);
        }
    }

    @Nested
    @DisplayName("enable / disable")
    class EstadoHabilitado {

        @Test
        @DisplayName("disable apaga la asignacion")
        void disable_apaga_la_asignacion() {
            EmployeeRole employeeRole = EmployeeRoleMother.habilitado();

            employeeRole.disable();

            assertThat(employeeRole.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable reactiva una asignacion deshabilitada")
        void enable_reactiva_una_asignacion_deshabilitada() {
            EmployeeRole employeeRole = EmployeeRoleMother.deshabilitado();

            employeeRole.enable();

            assertThat(employeeRole.isEnabled()).isTrue();
        }
    }
}
