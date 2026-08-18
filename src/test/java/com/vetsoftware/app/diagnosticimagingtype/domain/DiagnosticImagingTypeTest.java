package com.vetsoftware.app.diagnosticimagingtype.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("DiagnosticImagingType — invariantes y ciclo de vida")
class DiagnosticImagingTypeTest {

    private static final CompanyRef EMPRESA = new CompanyRef(9L, "Clinica Norte", "900123456");
    private static final LocalDateTime CREATED_DATE = LocalDateTime.of(2026, 1, 15, 10, 0);

    private static Builder valida() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 501L;
        private String name = "Radiografia";
        private String description = "Radiografia simple digital";
        private CompanyRef company;
        private boolean general = true;
        private LocalDateTime createdDate = CREATED_DATE;
        private boolean enabled = true;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder description(String v) {
            this.description = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Builder general(boolean v) {
            this.general = v;
            return this;
        }

        private DiagnosticImagingType build() {
            return new DiagnosticImagingType(id, name, description, company, general, createdDate,
                    enabled);
        }
    }

    @Nested
    @DisplayName("constructor")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo en su sitio")
        void conserva_cada_campo_en_su_sitio() {
            DiagnosticImagingType type = valida().build();

            assertThat(type.getId()).isEqualTo(501L);
            assertThat(type.getName()).isEqualTo("Radiografia");
            assertThat(type.getDescription()).isEqualTo("Radiografia simple digital");
            assertThat(type.getCompany()).isNull();
            assertThat(type.isGeneral()).isTrue();
            assertThat(type.getCreatedDate()).isEqualTo(CREATED_DATE);
            assertThat(type.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un tipo propio de empresa conserva su company")
        void un_tipo_propio_de_empresa_conserva_su_company() {
            DiagnosticImagingType type = valida().general(false).company(EMPRESA).build();

            assertThat(type.getCompany()).isEqualTo(EMPRESA);
            assertThat(type.isGeneral()).isFalse();
        }
    }

    @Nested
    @DisplayName("create")
    class Creacion {

        @Test
        @DisplayName("un tipo nuevo nace sin id, habilitado y con fecha de creacion")
        void un_tipo_nuevo_nace_sin_id_habilitado_y_con_fecha() {
            DiagnosticImagingType type = DiagnosticImagingType.create("Ecografia",
                    "Ecografia abdominal", EMPRESA, false);

            assertThat(type.getId()).isNull();
            assertThat(type.isEnabled()).isTrue();
            assertThat(type.getCreatedDate()).isNotNull();
            assertThat(type.getName()).isEqualTo("Ecografia");
            assertThat(type.getCompany()).isEqualTo(EMPRESA);
        }

        @Test
        @DisplayName("create valida igual que el constructor")
        void create_valida_igual_que_el_constructor() {
            assertThatThrownBy(() -> DiagnosticImagingType.create(null, "desc", null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }
    }

    @Nested
    @DisplayName("validaciones de invariantes")
    class Validaciones {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("nombre nulo", (ThrowingCallable) () -> valida().name(null).build(),
                            "name is required"),
                    arguments("nombre en blanco",
                            (ThrowingCallable) () -> valida().name("  ").build(),
                            "name is required"),
                    arguments("nombre de mas de 100 caracteres",
                            (ThrowingCallable) () -> valida().name("A".repeat(101)).build(),
                            "name must be 100 chars or less"),
                    arguments("descripcion de mas de 500 caracteres",
                            (ThrowingCallable) () -> valida().description("A".repeat(501)).build(),
                            "description must be 500 chars or less"),
                    arguments("tipo general con company asociada",
                            (ThrowingCallable) () -> valida().general(true).company(EMPRESA)
                                    .build(),
                            "general type cannot have company"),
                    arguments("tipo no general sin company",
                            (ThrowingCallable) () -> valida().general(false).company(null).build(),
                            "non-general type requires company"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("cada invariante violada lanza IllegalArgumentException con su mensaje")
        void cada_invariante_violada_lanza_excepcion(String descripcion, ThrowingCallable accion,
                String mensajeEsperado) {
            assertThatThrownBy(accion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensajeEsperado);
        }

        @Test
        @DisplayName("nombre en el limite de 100 caracteres se acepta")
        void nombre_en_el_limite_se_acepta() {
            assertThatCode(() -> valida().name("A".repeat(100)).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("descripcion en el limite de 500 caracteres se acepta")
        void descripcion_en_el_limite_se_acepta() {
            assertThatCode(() -> valida().description("A".repeat(500)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("descripcion nula se acepta (es opcional)")
        void descripcion_nula_se_acepta() {
            assertThatCode(() -> valida().description(null).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Actualizacion {

        @Test
        @DisplayName("reemplaza los campos editables")
        void reemplaza_los_campos_editables() {
            DiagnosticImagingType type = valida().build();

            type.update("Radiografia digital", "Descripcion actualizada", null, true);

            assertThat(type.getName()).isEqualTo("Radiografia digital");
            assertThat(type.getDescription()).isEqualTo("Descripcion actualizada");
        }

        @Test
        @DisplayName("update valida igual que el constructor")
        void update_valida_igual_que_el_constructor() {
            DiagnosticImagingType type = valida().build();

            assertThatThrownBy(() -> type.update("", "desc", null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("update conserva el id original (no se puede reasignar)")
        void update_conserva_el_id_original() {
            DiagnosticImagingType type = valida().build();

            type.update("Nuevo nombre", "desc", null, true);

            assertThat(type.getId()).isEqualTo(501L);
        }
    }

    @Nested
    @DisplayName("enable / disable")
    class Estado {

        @Test
        @DisplayName("disable apaga el tipo")
        void disable_apaga_el_tipo() {
            DiagnosticImagingType type = valida().build();

            type.disable();

            assertThat(type.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable vuelve a encender el tipo")
        void enable_vuelve_a_encender_el_tipo() {
            DiagnosticImagingType type = valida().build();
            type.disable();

            type.enable();

            assertThat(type.isEnabled()).isTrue();
        }
    }
}
