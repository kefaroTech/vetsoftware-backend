package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Sugerencia del código de acceso del empleado. El código es único GLOBAL y es
 * el usuario con el que la persona inicia sesión: si la sugerencia colisiona o
 * excede el límite de la columna, el alta falla en medio de la transacción de
 * invitación.
 */
@DisplayName("EmployeeCodeGenerator")
class EmployeeCodeGeneratorTest {

    /**
     * Prefijo que produce {@link EmployeeMother#RAZON_SOCIAL_LARGA}: una inicial
     * por palabra.
     */
    private static final String PREFIJO_LARGO = "P".repeat(45);

    /**
     * La base sin recortar mide 56 caracteres ({@code 45 + "-" + "MAXIMILIAN"}) y
     * el recorte a 50 se come seis letras del nombre.
     */
    private static final String BASE_SIN_RECORTAR = PREFIJO_LARGO + "-MAXIMILIAN";
    private static final String BASE_RECORTADA = PREFIJO_LARGO + "-MAXI";

    private static Predicate<String> taken(String... codes) {
        Set<String> set = Set.of(codes);
        return set::contains;
    }

    @Nested
    @DisplayName("Formato")
    class Formato {

        /**
         * En la fila de "Vet 24/7 S.A.S." el "24/7" desaparece por completo y "S.A.S."
         * se colapsa en una sola palabra, así que aporta una única inicial S.
         */
        @ParameterizedTest(name = "[{index}] \"{0}\" + \"{1}\" -> \"{2}\"")
        @CsvSource({"Veterinaria Vetrina, Orlando Velasquez, VV-ORLANDOVEL",
                "Clinica Animal, Mariaalejandra Rodriguez, CA-MARIAALEJA",
                "Ángeles Peludos, Iñaki Muñoz, AP-INAKIMUNOZ",
                "Vet 24/7 S.A.S., Ana-Maria 2, VS-ANAMARIA",
                "Vet   Central, Juan    Perez, VC-JUANPEREZ",
                "Vet Central, Juan Perez, VC-JUANPEREZ"})
        @DisplayName("compone iniciales de la empresa y nombre sin espacios, tildes ni símbolos")
        void compone_iniciales_de_empresa_y_nombre_normalizado(String empresa, String nombre,
                String esperado) {
            assertThat(EmployeeCodeGenerator.generate(empresa, nombre)).isEqualTo(esperado);
        }

        @Test
        @DisplayName("tolera empresa y nombre nulos o vacíos sin reventar")
        void tolera_nombres_nulos_o_vacios_sin_reventar() {
            assertThat(EmployeeCodeGenerator.generate(null, null)).isEqualTo("-");
            assertThat(EmployeeCodeGenerator.generate("", "")).isEqualTo("-");
        }

        /**
         * El corte del nombre es {@code > 10}, no {@code >= 10}: diez caracteres pasan
         * enteros y el recorte solo entra a partir del once.
         */
        @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
        @CsvSource({"Alexandria, VC-ALEXANDRIA", "Alexandrina, VC-ALEXANDRIN"})
        @DisplayName("recorta el nombre a partir del carácter once, no en el diez")
        void recorta_el_nombre_a_partir_del_caracter_once(String nombre, String esperado) {
            assertThat(EmployeeCodeGenerator.generate("Vet Central", nombre)).isEqualTo(esperado);
        }

        /**
         * Una razón social sin ninguna letra deja el prefijo vacío y el código empieza
         * por el guion separador. Es un código legal: sigue siendo único.
         */
        @ParameterizedTest(name = "[{index}] empresa \"{0}\"")
        @ValueSource(strings = {"24/7", "911", "+++", "-.-"})
        @DisplayName("una empresa sin letras deja el prefijo vacío y el código abre con guion")
        void empresa_sin_letras_deja_el_prefijo_vacio(String empresa) {
            String code = EmployeeCodeGenerator.generate(empresa, "Juan Perez");

            assertThat(code).isEqualTo("-JUANPEREZ").startsWith("-");
        }

        @Test
        @DisplayName("una empresa con dígitos conserva las letras que sí tiene")
        void una_empresa_con_digitos_conserva_sus_letras() {
            assertThat(EmployeeCodeGenerator.generate("3M", "Juan Perez")).isEqualTo("M-JUANPEREZ");
        }

        /**
         * "Иван Петров" y "王小明": el normalizador solo conserva {@code [A-Za-z]}, así
         * que un nombre íntegramente no latino se queda sin sufijo y el código es solo
         * el prefijo más el guion.
         */
        @ParameterizedTest(name = "[{index}] nombre \"{0}\"")
        @ValueSource(strings = {"Иван Петров", "王小明"})
        @DisplayName("un nombre íntegramente no latino deja el sufijo vacío")
        void un_nombre_no_latino_deja_el_sufijo_vacio(String nombre) {
            String code = EmployeeCodeGenerator.generate("Vet Central", nombre);

            assertThat(code).isEqualTo("VC-").endsWith("-");
        }
    }

    @Nested
    @DisplayName("Disponibilidad")
    class Disponibilidad {

        @Test
        @DisplayName("devuelve la base tal cual cuando está libre")
        void devuelve_la_base_cuando_esta_libre() {
            String code = EmployeeCodeGenerator.generateAvailable("Veterinaria Vetrina",
                    "Orlando Velasquez", taken());

            assertThat(code).isEqualTo("VV-ORLANDOVEL");
        }

        @Test
        @DisplayName("agrega el sufijo -2 cuando la base ya está tomada")
        void agrega_sufijo_2_cuando_la_base_esta_tomada() {
            String code = EmployeeCodeGenerator.generateAvailable("Veterinaria Vetrina",
                    "Orlando Velasquez", taken("VV-ORLANDOVEL"));

            assertThat(code).isEqualTo("VV-ORLANDOVEL-2");
        }

        @Test
        @DisplayName("sigue incrementando el sufijo hasta encontrar uno libre")
        void sigue_incrementando_el_sufijo_hasta_encontrar_uno_libre() {
            String code = EmployeeCodeGenerator.generateAvailable("Veterinaria Vetrina",
                    "Orlando Velasquez",
                    taken("VV-ORLANDOVEL", "VV-ORLANDOVEL-2", "VV-ORLANDOVEL-3"));

            assertThat(code).isEqualTo("VV-ORLANDOVEL-4");
        }

        @Test
        @DisplayName("evalúa el predicado sobre el candidato exacto, no sobre su prefijo")
        void el_predicado_de_ocupado_se_evalua_sobre_el_candidato_exacto() {
            // Evita el falso positivo de comparar prefijos: "VV-JUANPEREZ" tomado no
            // bloquea "VV-JUANPEREZ-2".
            String code = EmployeeCodeGenerator.generateAvailable("Vet Vetrina", "Juan Perez",
                    taken("VV-JUANPEREZ"));

            assertThat(code).isEqualTo("VV-JUANPEREZ-2");
        }

        @Test
        @DisplayName("se rinde de forma explícita si todo está tomado")
        void se_rinde_de_forma_explicita_si_todo_esta_tomado() {
            assertThatThrownBy(() -> EmployeeCodeGenerator.generateAvailable("Vet Central",
                    "Juan Perez", codigo -> true)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("unique employee code");
        }
    }

    /**
     * El límite de {@code employee_code(50)} se aplica ANTES de consultar
     * disponibilidad, no solo en la rama del sufijo: el candidato que se consulta
     * tiene que ser exactamente el que se devuelve y se persiste. Antes del
     * arreglo, una razón social larga con la base libre devolvía 56 caracteres y
     * reventaba la invariante de {@code Employee} en mitad de la transacción de
     * invitación.
     */
    @Nested
    @DisplayName("Límite de employee_code(50)")
    class LimiteDeLaColumna {

        @Test
        @DisplayName("la razón social larga produce una base que no cabe en la columna")
        void la_razon_social_larga_desborda_la_columna() {
            String base = EmployeeCodeGenerator.generate(EmployeeMother.RAZON_SOCIAL_LARGA,
                    "Maximiliano");

            assertThat(base).isEqualTo(BASE_SIN_RECORTAR).hasSize(56);
        }

        @Test
        @DisplayName("con la base libre devuelve 50 caracteres exactos y sin sufijo")
        void con_la_base_libre_recorta_a_cincuenta_sin_sufijo() {
            String code = EmployeeCodeGenerator.generateAvailable(EmployeeMother.RAZON_SOCIAL_LARGA,
                    "Maximiliano", taken());

            assertThat(code).isEqualTo(BASE_RECORTADA).hasSize(50).doesNotEndWith("-2");
        }

        @Test
        @DisplayName("consulta la base ya recortada, no la de 56 caracteres")
        void consulta_la_base_ya_recortada() {
            // Marcar como tomada la cadena SIN recortar no debe bloquear nada: esa cadena
            // nunca llega a ser candidata.
            String code = EmployeeCodeGenerator.generateAvailable(EmployeeMother.RAZON_SOCIAL_LARGA,
                    "Maximiliano", taken(BASE_SIN_RECORTAR));

            assertThat(code).isEqualTo(BASE_RECORTADA).hasSize(50);
        }

        @Test
        @DisplayName("si la base recortada está tomada, el sufijo cabe recortando más")
        void el_sufijo_cabe_recortando_la_base_recortada() {
            String code = EmployeeCodeGenerator.generateAvailable(EmployeeMother.RAZON_SOCIAL_LARGA,
                    "Maximiliano", taken(BASE_RECORTADA));

            assertThat(code).isEqualTo(PREFIJO_LARGO + "-MA-2").hasSize(50).endsWith("-2");
        }

        /**
         * Al pasar de una a dos cifras la reserva sube de 2 a 3 caracteres, así que la
         * base se recorta a 47 en vez de a 48 y el candidato cambia de forma: es el
         * salto que ningún test llegaba a ejercitar.
         */
        @Test
        @DisplayName("al saltar del sufijo -9 al -10 recorta un carácter más")
        void al_saltar_a_dos_cifras_recorta_un_caracter_mas() {
            String unaCifra = PREFIJO_LARGO + "-MA";

            String code = EmployeeCodeGenerator.generateAvailable(EmployeeMother.RAZON_SOCIAL_LARGA,
                    "Maximiliano",
                    taken(BASE_RECORTADA, unaCifra + "-2", unaCifra + "-3", unaCifra + "-4",
                            unaCifra + "-5", unaCifra + "-6", unaCifra + "-7", unaCifra + "-8",
                            unaCifra + "-9"));

            assertThat(code).isEqualTo(PREFIJO_LARGO + "-M-10").hasSize(50).endsWith("-10");
        }
    }
}
