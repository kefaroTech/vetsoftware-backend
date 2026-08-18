package com.vetsoftware.app.employee.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Invariantes del agregado Employee: es la identidad con la que alguien inicia
 * sesión, así que sus reglas de creación, cambio de contraseña y ciclo
 * INVITED→ACTIVE son control de acceso, no solo validación de formulario. Se
 * fija además qué operaciones invalidan sesiones vivas (authVersion) y cuáles
 * no.
 */
class EmployeeTest {

    private static CompanyRef company() {
        return new CompanyRef(9L, "Veterinaria Vetrina", "900123456");
    }

    private static Employee valid() {
        return Employee.create("VV-ORLANDO", "$2a$10$hash", "Orlando Velásquez",
                "orlando@vetrina.co", company(), true, false);
    }

    @Nested
    class Creacion {

        @Test
        void el_staff_invitado_nace_invited_y_obligado_a_cambiar_la_clave() {
            Employee employee = Employee.create("VV-MARIANA", "$2a$10$hash", "Mariana Rojas",
                    "mariana@vetrina.co", company(), true, true);

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.INVITED);
            assertThat(employee.isMustChangePassword()).isTrue();
            assertThat(employee.isEmailVerified()).isTrue();
        }

        @Test
        void el_dueno_autoregistrado_nace_activo_sin_verificar_el_correo() {
            Employee employee = Employee.create("VV-ORLANDO", "$2a$10$hash", "Orlando",
                    "orlando@vetrina.co", company(), false, false);

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(employee.isEmailVerified()).as("Opción B: debe verificar antes de entrar")
                    .isFalse();
            assertThat(employee.isMustChangePassword()).isFalse();
        }

        @Test
        void nace_habilitado_y_con_authVersion_cero() {
            Employee employee = valid();

            assertThat(employee.isEnabled()).isTrue();
            assertThat(employee.getAuthVersion()).isZero();
            assertThat(employee.getId()).as("el id lo asigna la BD").isNull();
        }

        @Test
        void nunca_guarda_la_contrasena_en_claro() {
            // El agregado recibe SIEMPRE un hash ya calculado; no tiene forma de hashear
            // por sí mismo.
            Employee employee = valid();

            assertThat(employee.getHashPassword()).startsWith("$2a$");
        }
    }

    @Nested
    class InvariantesDeConstruccion {

        @Test
        void exige_codigo_de_empleado() {
            assertThatThrownBy(
                    () -> Employee.create(null, "h", "n", "e@e.co", company(), true, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employeeCode is required");
            assertThatThrownBy(
                    () -> Employee.create("  ", "h", "n", "e@e.co", company(), true, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void limita_el_codigo_a_50_caracteres() {
            assertThatThrownBy(() -> Employee.create("X".repeat(51), "h", "n", "e@e.co", company(),
                    true, false)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("50 chars or less");

            assertThatCode(() -> Employee.create("X".repeat(50), "h", "n", "e@e.co", company(),
                    true, false)).doesNotThrowAnyException();
        }

        @Test
        void exige_hash_de_contrasena() {
            assertThatThrownBy(
                    () -> Employee.create("C", null, "n", "e@e.co", company(), true, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password is required");
            assertThatThrownBy(
                    () -> Employee.create("C", "   ", "n", "e@e.co", company(), true, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void exige_nombre_y_correo_dentro_de_100_caracteres() {
            assertThatThrownBy(
                    () -> Employee.create("C", "h", "", "e@e.co", company(), true, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
            assertThatThrownBy(() -> Employee.create("C", "h", "N".repeat(101), "e@e.co", company(),
                    true, false)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Employee.create("C", "h", "n", null, company(), true, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email is required");
            assertThatThrownBy(
                    () -> Employee.create("C", "h", "n", "e".repeat(101), company(), true, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void exige_empresa() {
            assertThatThrownBy(() -> Employee.create("C", "h", "n", "e@e.co", null, true, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }

        @Test
        void la_referencia_de_empresa_valida_sus_propios_campos() {
            assertThatThrownBy(() -> new CompanyRef(null, "n", "i"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompanyRef(1L, " ", "i"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CompanyRef(1L, "n", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CambioDeContrasena {

        @Test
        void cambiar_la_clave_limpia_la_obligacion_de_cambiarla() {
            Employee employee = Employee.create("C", "$2a$10$old", "n", "e@e.co", company(), true,
                    true);

            employee.changePassword("$2a$10$new");

            assertThat(employee.getHashPassword()).isEqualTo("$2a$10$new");
            assertThat(employee.isMustChangePassword()).isFalse();
        }

        @Test
        void cambiar_la_clave_NO_invalida_las_sesiones_vivas() {
            // Comportamiento actual documentado: authVersion no sube, así que la sesión en
            // curso
            // sobrevive
            // (y con ella cualquier refresh token emitido antes del cambio).
            Employee employee = Employee.create("C", "$2a$10$old", "n", "e@e.co", company(), true,
                    true);
            Long before = employee.getAuthVersion();

            employee.changePassword("$2a$10$new");

            assertThat(employee.getAuthVersion()).isEqualTo(before);
        }

        @Test
        void restablecer_la_clave_si_invalida_las_sesiones_vivas() {
            Employee employee = valid();

            employee.resetPassword("$2a$10$reset");

            assertThat(employee.getHashPassword()).isEqualTo("$2a$10$reset");
            assertThat(employee.isMustChangePassword()).isFalse();
            assertThat(employee.getAuthVersion()).isEqualTo(1L);
        }

        @Test
        void restablecer_dos_veces_sube_la_version_dos_veces() {
            Employee employee = valid();

            employee.resetPassword("$2a$10$a");
            employee.resetPassword("$2a$10$b");

            assertThat(employee.getAuthVersion()).isEqualTo(2L);
        }

        @Test
        void no_acepta_una_clave_vacia() {
            Employee employee = valid();

            assertThatThrownBy(() -> employee.changePassword(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> employee.changePassword("  "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> employee.resetPassword(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CicloDeVida {

        @Test
        void activar_es_idempotente() {
            Employee employee = Employee.create("C", "h", "n", "e@e.co", company(), true, true);

            employee.activate();
            employee.activate();

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        }

        @Test
        void verificar_el_correo_es_idempotente() {
            Employee employee = Employee.create("C", "h", "n", "e@e.co", company(), false, false);

            employee.verifyEmail();
            employee.verifyEmail();

            assertThat(employee.isEmailVerified()).isTrue();
        }

        @Test
        void reinvitar_devuelve_al_empleado_al_estado_invited_con_clave_temporal() {
            Employee employee = valid();
            employee.activate();

            employee.reinvite("$2a$10$temp");

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.INVITED);
            assertThat(employee.isMustChangePassword()).isTrue();
            assertThat(employee.getHashPassword()).isEqualTo("$2a$10$temp");
        }

        @Test
        void reinvitar_exige_una_clave_temporal() {
            Employee employee = valid();

            assertThatThrownBy(() -> employee.reinvite(" "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void habilitar_y_deshabilitar_alternan_el_acceso() {
            Employee employee = valid();

            employee.disable();
            assertThat(employee.isEnabled()).isFalse();

            employee.enable();
            assertThat(employee.isEnabled()).isTrue();
        }
    }

    @Nested
    class Actualizacion {

        @Test
        void actualiza_codigo_nombre_y_correo() {
            Employee employee = valid();

            employee.update("VV-NUEVO", "Nombre Nuevo", "nuevo@vetrina.co");

            assertThat(employee.getEmployeeCode()).isEqualTo("VV-NUEVO");
            assertThat(employee.getName()).isEqualTo("Nombre Nuevo");
            assertThat(employee.getEmail()).isEqualTo("nuevo@vetrina.co");
        }

        @Test
        void no_permite_dejar_el_empleado_sin_codigo_nombre_o_correo() {
            Employee employee = valid();

            assertThatThrownBy(() -> employee.update(" ", "n", "e@e.co"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> employee.update("C", " ", "e@e.co"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> employee.update("C", "n", " "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void actualizar_no_puede_cambiar_la_empresa_del_empleado() {
            // La empresa es final: un empleado no se "muda" de tenant.
            Employee employee = valid();

            employee.update("VV-NUEVO", "Nombre Nuevo", "nuevo@vetrina.co");

            assertThat(employee.getCompany().id()).isEqualTo(9L);
        }

        @Test
        void no_permite_un_codigo_de_mas_de_50_caracteres() {
            Employee employee = valid();

            assertThatThrownBy(() -> employee.update("X".repeat(51), "n", "e@e.co"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("50 chars or less");

            assertThatCode(() -> employee.update("X".repeat(50), "n", "e@e.co"))
                    .doesNotThrowAnyException();
        }

        @Test
        void no_permite_un_nombre_de_mas_de_100_caracteres() {
            Employee employee = valid();

            assertThatThrownBy(() -> employee.update("C", "N".repeat(101), "e@e.co"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100 chars or less");

            assertThatCode(() -> employee.update("C", "N".repeat(100), "e@e.co"))
                    .doesNotThrowAnyException();
        }

        @Test
        void no_permite_un_correo_de_mas_de_100_caracteres() {
            Employee employee = valid();

            assertThatThrownBy(() -> employee.update("C", "n", "e".repeat(101)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100 chars or less");

            assertThatCode(() -> employee.update("C", "n", "e".repeat(100)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class ValoresPorDefectoDelConstructor {

        @Test
        void un_status_nulo_se_normaliza_a_active() {
            Employee employee = new Employee(1L, "C", "h", "n", "e@e.co", company(), null, true,
                    true, false, null, 0L);

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        }

        @Test
        void un_auth_version_nulo_se_normaliza_a_cero() {
            Employee employee = new Employee(1L, "C", "h", "n", "e@e.co", company(), null, true,
                    true, false, EmployeeStatus.ACTIVE, null);

            assertThat(employee.getAuthVersion()).isZero();
        }
    }
}
