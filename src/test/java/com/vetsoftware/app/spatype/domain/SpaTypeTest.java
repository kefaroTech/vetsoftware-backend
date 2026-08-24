package com.vetsoftware.app.spatype.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Primeras pruebas del dominio de {@code spatype}. El slice se creo en mayo de
 * 2026 y llego hasta aqui sin una sola clase de test: {@code SpaType} tenia 31
 * lineas medidas y cero cubiertas, asi que sus cuatro invariantes nunca se
 * ejecutaron fuera de produccion.
 *
 * <p>
 * Lo que se fija aqui es que {@code update} valida <strong>igual</strong> que
 * el constructor. Es el defecto natural de esta forma —dos caminos de escritura
 * y una sola funcion de validacion— y el sintoma seria un tipo de spa
 * renombrado por un PATCH a cadena vacia o a 400 caracteres, que el constructor
 * habria rechazado pero que entra por la puerta de al lado y revienta contra la
 * columna al hacer flush, con la transaccion ya a medias.
 */
@DisplayName("SpaType — invariantes del tipo de spa")
class SpaTypeTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 10, 8, 30);

    private static SpaType tipoValido() {
        return new SpaType(4L, "Bano medicado", "Con champu dermatologico", CREADO, 0L, true);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("conserva todos los campos que recibe")
        void conserva_todos_los_campos() {
            SpaType tipo = new SpaType(4L, "Bano medicado", "Con champu", CREADO, 2L, false);

            assertThat(tipo.getId()).isEqualTo(4L);
            assertThat(tipo.getName()).isEqualTo("Bano medicado");
            assertThat(tipo.getDescription()).isEqualTo("Con champu");
            assertThat(tipo.getCreatedDate()).isEqualTo(CREADO);
            assertThat(tipo.getVersion()).isEqualTo(2L);
            assertThat(tipo.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("un tipo recien creado nace habilitado y sin id ni version")
        void un_tipo_recien_creado_nace_habilitado() {
            SpaType tipo = SpaType.create("Corte de pelo", "Raza pequena");

            assertThat(tipo.getId()).isNull();
            assertThat(tipo.getVersion()).isNull();
            assertThat(tipo.isEnabled()).isTrue();
            assertThat(tipo.getName()).isEqualTo("Corte de pelo");
            // getCreatedDate no se afirma: create() llama a LocalDateTime.now() y no
            // acepta Clock. Es la misma deuda registrada de Animal.create; el dia que
            // se inyecte el reloj, este es el sitio donde se afirma.
            assertThat(tipo.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("la descripcion es opcional")
        void la_descripcion_es_opcional() {
            SpaType tipo = SpaType.create("Solo bano", null);

            assertThat(tipo.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("el nombre no puede faltar ni venir en blanco")
        void el_nombre_no_puede_venir_en_blanco(String nombre) {
            assertThatThrownBy(() -> SpaType.create(nombre, "cualquiera"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("el nombre no pasa de 100 caracteres")
        void el_nombre_no_pasa_de_cien() {
            String demasiado = "x".repeat(101);

            assertThatThrownBy(() -> SpaType.create(demasiado, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100 chars or less");
        }

        @Test
        @DisplayName("exactamente 100 caracteres si entra: el limite es inclusivo")
        void exactamente_cien_caracteres_entra() {
            assertThatCode(() -> SpaType.create("x".repeat(100), null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("la descripcion no pasa de 500 caracteres")
        void la_descripcion_no_pasa_de_quinientos() {
            String demasiado = "y".repeat(501);

            assertThatThrownBy(() -> SpaType.create("Bano", demasiado))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("500 chars or less");
        }

        @Test
        @DisplayName("exactamente 500 caracteres de descripcion si entra")
        void exactamente_quinientos_de_descripcion_entra() {
            assertThatCode(() -> SpaType.create("Bano", "y".repeat(500)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("cambia nombre y descripcion y no toca el resto del estado")
        void cambia_nombre_y_descripcion_sin_tocar_el_resto() {
            SpaType tipo = tipoValido();

            tipo.update("Bano hipoalergenico", "Piel sensible");

            assertThat(tipo.getName()).isEqualTo("Bano hipoalergenico");
            assertThat(tipo.getDescription()).isEqualTo("Piel sensible");
            assertThat(tipo.getId()).isEqualTo(4L);
            assertThat(tipo.getCreatedDate()).isEqualTo(CREADO);
            assertThat(tipo.isEnabled()).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("update valida el nombre igual que el constructor")
        void update_valida_el_nombre_igual_que_el_constructor(String nombre) {
            SpaType tipo = tipoValido();

            assertThatThrownBy(() -> tipo.update(nombre, "algo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("update valida la longitud igual que el constructor")
        void update_valida_la_longitud_igual_que_el_constructor() {
            SpaType tipo = tipoValido();

            assertThatThrownBy(() -> tipo.update("z".repeat(101), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100 chars or less");
            assertThatThrownBy(() -> tipo.update("Bano", "z".repeat(501)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("500 chars or less");
        }

        @Test
        @DisplayName("un update rechazado deja el tipo como estaba")
        void un_update_rechazado_deja_el_tipo_como_estaba() {
            SpaType tipo = tipoValido();

            assertThatThrownBy(() -> tipo.update("  ", "nueva"))
                    .isInstanceOf(IllegalArgumentException.class);

            // validate() corre ANTES de asignar. Si algun dia se reordena, el tipo
            // quedaria con la descripcion nueva y el nombre viejo: medio update
            // aplicado sobre un objeto que la transaccion va a persistir igual.
            assertThat(tipo.getName()).isEqualTo("Bano medicado");
            assertThat(tipo.getDescription()).isEqualTo("Con champu dermatologico");
        }
    }

    @Nested
    @DisplayName("Habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("deshabilitar y volver a habilitar es idempotente")
        void deshabilitar_y_habilitar_es_idempotente() {
            SpaType tipo = tipoValido();

            tipo.disable();
            assertThat(tipo.isEnabled()).isFalse();
            tipo.disable();
            assertThat(tipo.isEnabled()).isFalse();

            tipo.enable();
            assertThat(tipo.isEnabled()).isTrue();
            tipo.enable();
            assertThat(tipo.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("deshabilitar no borra nada: el nombre y la descripcion siguen ahi")
        void deshabilitar_no_borra_nada() {
            SpaType tipo = tipoValido();

            tipo.disable();

            assertThat(tipo.getName()).isEqualTo("Bano medicado");
            assertThat(tipo.getDescription()).isEqualTo("Con champu dermatologico");
            assertThat(tipo.getCreatedDate()).isEqualTo(CREADO);
        }
    }
}
