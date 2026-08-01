package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sugerencia del código de acceso del empleado. El código es único GLOBAL y es el usuario con el que la
 * persona inicia sesión: si la sugerencia colisiona o excede el límite de la columna, el alta falla en
 * medio de la transacción de invitación.
 */
class EmployeeCodeGeneratorTest {

    private static Predicate<String> taken(String... codes) {
        Set<String> set = Set.of(codes);
        return set::contains;
    }

    @Nested
    class Formato {

        @Test
        void usa_las_iniciales_de_la_empresa_y_el_nombre_sin_espacios() {
            assertThat(EmployeeCodeGenerator.generate("Veterinaria Vetrina", "Orlando Velasquez"))
                    .isEqualTo("VV-ORLANDOVEL");
        }

        @Test
        void recorta_el_nombre_a_diez_caracteres() {
            String code = EmployeeCodeGenerator.generate("Clinica Animal", "Mariaalejandra Rodriguez");

            assertThat(code).isEqualTo("CA-MARIAALEJA");
            assertThat(code.substring(code.indexOf('-') + 1)).hasSize(10);
        }

        @Test
        void quita_tildes_y_enes() {
            assertThat(EmployeeCodeGenerator.generate("Ángeles Peludos", "Iñaki Muñoz"))
                    .isEqualTo("AP-INAKIMUNOZ");
        }

        @Test
        void descarta_digitos_y_simbolos() {
            // "24/7" desaparece por completo y "S.A.S." se colapsa en una sola palabra → inicial S.
            assertThat(EmployeeCodeGenerator.generate("Vet 24/7 S.A.S.", "Ana-Maria 2"))
                    .isEqualTo("VS-ANAMARIA");
        }

        @Test
        void tolera_nombres_nulos_o_vacios_sin_reventar() {
            assertThat(EmployeeCodeGenerator.generate(null, null)).isEqualTo("-");
            assertThat(EmployeeCodeGenerator.generate("", "")).isEqualTo("-");
        }

        @Test
        void colapsa_espacios_multiples() {
            assertThat(EmployeeCodeGenerator.generate("Vet   Central", "Juan    Perez"))
                    .isEqualTo("VC-JUANPEREZ");
        }
    }

    @Nested
    class Disponibilidad {

        @Test
        void devuelve_la_base_cuando_esta_libre() {
            String code = EmployeeCodeGenerator.generateAvailable(
                    "Veterinaria Vetrina", "Orlando Velasquez", taken());

            assertThat(code).isEqualTo("VV-ORLANDOVEL");
        }

        @Test
        void agrega_sufijo_2_cuando_la_base_esta_tomada() {
            String code = EmployeeCodeGenerator.generateAvailable(
                    "Veterinaria Vetrina", "Orlando Velasquez", taken("VV-ORLANDOVEL"));

            assertThat(code).isEqualTo("VV-ORLANDOVEL-2");
        }

        @Test
        void sigue_incrementando_el_sufijo_hasta_encontrar_uno_libre() {
            String code = EmployeeCodeGenerator.generateAvailable(
                    "Veterinaria Vetrina", "Orlando Velasquez",
                    taken("VV-ORLANDOVEL", "VV-ORLANDOVEL-2", "VV-ORLANDOVEL-3"));

            assertThat(code).isEqualTo("VV-ORLANDOVEL-4");
        }

        @Test
        void nunca_supera_los_50_caracteres_de_la_columna() {
            // Una razón social muy larga produce un prefijo de una inicial por palabra: la base ya roza el
            // límite y al añadir el sufijo hay que recortar, no desbordar la columna employee_code(50).
            StringBuilder empresaLarga = new StringBuilder();
            for (int i = 0; i < 45; i++) {
                empresaLarga.append("Palabra").append(i % 2 == 0 ? "x " : "y ");
            }
            String base = EmployeeCodeGenerator.generate(empresaLarga.toString(), "Maximiliano");
            assertThat(base.length()).isGreaterThan(50);

            String code = EmployeeCodeGenerator.generateAvailable(
                    empresaLarga.toString(), "Maximiliano", taken(base));

            assertThat(code).endsWith("-2");
            assertThat(code.length()).isLessThanOrEqualTo(50);
        }

        @Test
        void se_rinde_de_forma_explicita_si_todo_esta_tomado() {
            assertThatThrownBy(() -> EmployeeCodeGenerator.generateAvailable(
                    "Vet Central", "Juan Perez", codigo -> true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("unique employee code");
        }

        @Test
        void el_predicado_de_ocupado_se_evalua_sobre_el_candidato_exacto() {
            // Evita el falso positivo de comparar prefijos: "VV-JUANPEREZ" tomado no bloquea "VV-JUANPEREZ-2".
            String code = EmployeeCodeGenerator.generateAvailable(
                    "Vet Vetrina", "Juan Perez", taken("VV-JUANPEREZ"));

            assertThat(code).isEqualTo("VV-JUANPEREZ-2");
        }
    }
}
