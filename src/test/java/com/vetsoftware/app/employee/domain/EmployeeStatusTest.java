package com.vetsoftware.app.employee.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Ciclo de invitacion del empleado recorrido sobre TODOS los estados del enum.
 * El estado se persiste como texto ({@code EnumType.STRING}) y viaja al front
 * en el JSON, asi que renombrar una constante rompe filas ya escritas y
 * pantallas ya desplegadas: aqui queda clavado el nombre exacto.
 *
 * <p>
 * Complementa a {@code EmployeeTest}, que cubre las transiciones del camino
 * feliz; esta clase fija la ortogonalidad del estado frente a {@code enabled} y
 * {@code mustChangePassword}.
 */
@DisplayName("EmployeeStatus — ciclo de invitacion")
class EmployeeStatusTest {

    private static Employee conEstado(EmployeeStatus status) {
        return new Employee(EmployeeMother.EMPLOYEE_ID, "VV-MARIANA", EmployeeMother.HASH,
                "Mariana Rojas", "mariana@vetrina.co", EmployeeMother.VETRINA,
                EmployeeMother.CREADO, true, true, false, status, 0L);
    }

    @Nested
    @DisplayName("contrato del enum")
    class ContratoDelEnum {

        @Test
        @DisplayName("solo existen los dos estados del ciclo de invitacion")
        void solo_existen_los_dos_estados_del_ciclo_de_invitacion() {
            // Un tercer estado obliga a revisar el listado, el login y el reenvio de
            // invitacion:
            // que este test falle es la senal de que hay que hacerlo.
            assertThat(EmployeeStatus.values()).containsExactly(EmployeeStatus.INVITED,
                    EmployeeStatus.ACTIVE);
        }

        @ParameterizedTest
        @EnumSource(EmployeeStatus.class)
        @DisplayName("el nombre persistido sobrevive la ida y vuelta")
        void el_nombre_persistido_sobrevive_la_ida_y_vuelta(EmployeeStatus status) {
            assertThat(EmployeeStatus.valueOf(status.name())).isEqualTo(status);
        }
    }

    @Nested
    @DisplayName("transiciones")
    class Transiciones {

        @ParameterizedTest
        @EnumSource(EmployeeStatus.class)
        @DisplayName("activar deja el empleado en ACTIVE venga del estado que venga")
        void activar_deja_el_empleado_en_active(EmployeeStatus inicial) {
            Employee employee = conEstado(inicial);

            employee.activate();

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        }

        @ParameterizedTest
        @EnumSource(EmployeeStatus.class)
        @DisplayName("reinvitar devuelve a INVITED venga del estado que venga")
        void reinvitar_devuelve_a_invited(EmployeeStatus inicial) {
            Employee employee = conEstado(inicial);

            employee.reinvite("$2a$10$temporal");

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.INVITED);
            assertThat(employee.isMustChangePassword()).isTrue();
        }

        @Test
        @DisplayName("un estado nulo se guarda como ACTIVE y nunca como null")
        void un_estado_nulo_se_guarda_como_active() {
            // Filas viejas y mapeos parciales pueden traer null: el agregado no puede
            // quedar sin estado porque el DTO hace status.name().
            Employee employee = new Employee(1L, "C", "h", "n", "e@e.co", EmployeeMother.VETRINA,
                    LocalDateTime.of(2026, 1, 1, 0, 0), true, true, false, null, 0L);

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("ortogonalidad con enabled y mustChangePassword")
    class Ortogonalidad {

        @ParameterizedTest
        @EnumSource(EmployeeStatus.class)
        @DisplayName("desactivar al empleado no altera su estado de invitacion")
        void desactivar_no_altera_el_estado_de_invitacion(EmployeeStatus inicial) {
            Employee employee = conEstado(inicial);

            employee.disable();

            assertThat(employee.isEnabled()).isFalse();
            assertThat(employee.getStatus()).isEqualTo(inicial);
        }

        @Test
        @DisplayName("activar no limpia la obligacion de cambiar la contrasena")
        void activar_no_limpia_la_obligacion_de_cambiar_la_contrasena() {
            // El estado pasa a ACTIVE en el primer login; la obligacion se limpia recien
            // cuando cambia efectivamente la clave.
            Employee employee = new Employee(EmployeeMother.EMPLOYEE_ID, "VV-MARIANA",
                    EmployeeMother.HASH, "Mariana Rojas", "mariana@vetrina.co",
                    EmployeeMother.VETRINA, EmployeeMother.CREADO, true, true, true,
                    EmployeeStatus.INVITED, 0L);

            employee.activate();

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(employee.isMustChangePassword()).isTrue();
        }

        @Test
        @DisplayName("cambiar la contrasena no activa por si solo al empleado")
        void cambiar_la_contrasena_no_activa_por_si_solo_al_empleado() {
            Employee employee = conEstado(EmployeeStatus.INVITED);

            employee.changePassword("$2a$10$nueva");

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.INVITED);
        }

        @Test
        @DisplayName("reinvitar no reactiva a un empleado desactivado")
        void reinvitar_no_reactiva_a_un_empleado_desactivado() {
            Employee employee = EmployeeMother.deshabilitado();

            employee.reinvite("$2a$10$temporal");

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.INVITED);
            assertThat(employee.isEnabled()).isFalse();
        }
    }
}
