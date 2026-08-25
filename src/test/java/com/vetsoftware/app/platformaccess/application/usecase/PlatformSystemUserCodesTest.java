package com.vetsoftware.app.platformaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * El código con el que el superadministrador recién creado inicia sesión: las
 * cuentas de sistema entran por código, no por correo.
 *
 * <p>
 * <b>Por qué el camino de colisión importa tanto.</b> El {@code UNIQUE (code)}
 * de {@code system_users} es la última línea, y llegar a él significa una
 * excepción de clave duplicada dentro de la transacción que crea la cuenta: el
 * alta falla, la invitación queda sin consumir y la persona se queda mirando un
 * error genérico. El sufijo numérico es lo que evita ese desenlace, y esta
 * clase es el único sitio donde se comprueba.
 *
 * <p>
 * Y el techo de 50 caracteres no es decorativo: el código se trunca
 * <b>antes</b> de añadir el sufijo, para que {@code -2} no empuje el resultado
 * por encima del ancho de la columna.
 */
@DisplayName("PlatformSystemUserCodes — el código de login del superadministrador")
class PlatformSystemUserCodesTest {

    private static final Predicate<String> NINGUNO_TOMADO = code -> false;

    @Nested
    @DisplayName("forma del código")
    class Forma {

        @ParameterizedTest
        @CsvSource({"Ana Ramirez,SYS-ANARAMIREZ", "ana ramirez,SYS-ANARAMIREZ",
                "'  Ana   Ramirez  ',SYS-ANARAMIREZ", "José Ñáñez,SYS-JOSENANEZ",
                "Ana-Maria ONeill,SYS-ANAMARIAONEI"})
        @DisplayName("normaliza acentos, caja, espacios y signos, y antepone SYS-")
        void normaliza_el_nombre(String nombre, String esperado) {
            assertThat(PlatformSystemUserCodes.generateAvailable(nombre, NINGUNO_TOMADO))
                    .isEqualTo(esperado);
        }

        @Test
        @DisplayName("corta el nombre a 12 caracteres: el codigo no crece con el nombre")
        void corta_el_nombre_a_doce_caracteres() {
            String codigo = PlatformSystemUserCodes.generateAvailable("Maximiliano Buenaventura",
                    NINGUNO_TOMADO);

            assertThat(codigo).isEqualTo("SYS-MAXIMILIANOB").hasSize(16);
        }

        @ParameterizedTest
        @CsvSource({"'',SYS-ADMIN", "'   ',SYS-ADMIN", "1234,SYS-ADMIN", "'!!! ???',SYS-ADMIN"})
        @DisplayName("un nombre del que no queda ninguna letra cae al codigo de reserva")
        void un_nombre_sin_letras_cae_al_de_reserva(String nombre, String esperado) {
            // Sin este fallback el codigo seria «SYS-» pelado y dos altas asi
            // colisionarian entre si por una razon que nadie sabria leer.
            assertThat(PlatformSystemUserCodes.generateAvailable(nombre, NINGUNO_TOMADO))
                    .isEqualTo(esperado);
        }

        @Test
        @DisplayName("un nombre nulo cae al codigo de reserva en vez de reventar")
        void un_nombre_nulo_cae_al_de_reserva() {
            assertThat(PlatformSystemUserCodes.generateAvailable(null, NINGUNO_TOMADO))
                    .isEqualTo("SYS-ADMIN");
        }
    }

    @Nested
    @DisplayName("colisión — la alternativa es que el alta falle con clave duplicada")
    class Colision {

        @Test
        @DisplayName("si el codigo base esta tomado, prueba con el sufijo -2")
        void si_el_base_esta_tomado_prueba_con_dos() {
            Set<String> tomados = Set.of("SYS-ANARAMIREZ");

            assertThat(PlatformSystemUserCodes.generateAvailable("Ana Ramirez", tomados::contains))
                    .isEqualTo("SYS-ANARAMIREZ-2");
        }

        @Test
        @DisplayName("sigue subiendo el sufijo hasta encontrar uno libre")
        void sigue_subiendo_hasta_encontrar_uno_libre() {
            Set<String> tomados = Set.of("SYS-ANARAMIREZ", "SYS-ANARAMIREZ-2", "SYS-ANARAMIREZ-3");

            assertThat(PlatformSystemUserCodes.generateAvailable("Ana Ramirez", tomados::contains))
                    .isEqualTo("SYS-ANARAMIREZ-4");
        }

        @Test
        @DisplayName("con todo tomado se rinde con un error explicito, no con un bucle infinito")
        void con_todo_tomado_se_rinde_con_error_explicito() {
            assertThatThrownBy(
                    () -> PlatformSystemUserCodes.generateAvailable("Ana Ramirez", code -> true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Could not generate a free system user code");
        }
    }

    @Nested
    @DisplayName("el techo de la columna")
    class TechoDeLaColumna {

        @Test
        @DisplayName("ningun codigo generado supera los 50 caracteres de system_users.code")
        void ningun_codigo_supera_los_cincuenta() {
            String largo = "A".repeat(200);

            assertThat(PlatformSystemUserCodes.generateAvailable(largo, NINGUNO_TOMADO))
                    .hasSizeLessThanOrEqualTo(50);
        }

        @Test
        @DisplayName("el sufijo tampoco empuja el codigo por encima del techo")
        void el_sufijo_tampoco_empuja_por_encima_del_techo() {
            // Se trunca ANTES de anadir «-2»: al reves, el UPDATE fallaria por
            // longitud justo en el camino de colision, que es el menos probado.
            assertThat(PlatformSystemUserCodes.generateAvailable("A".repeat(200),
                    code -> code.equals("SYS-" + "A".repeat(12)))).hasSizeLessThanOrEqualTo(50)
                    .endsWith("-2");
        }
    }
}
