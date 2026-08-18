package com.vetsoftware.app.laboratorytestfile.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestStoragePathRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("StorageKeyFactory — construye la clave S3 de un archivo de examen")
class StorageKeyFactoryTest {

    private static final LaboratoryTestStoragePathRef RUTA = new LaboratoryTestStoragePathRef(9L,
            3L, 100L, "Firulais");

    @Nested
    @DisplayName("build()")
    class Build {

        @Test
        @DisplayName("compone companyId/ownerId/slug-animalId/uuid-nombre")
        void compone_la_ruta_completa() {
            String key = StorageKeyFactory.build(RUTA, "informe.pdf");

            assertThat(key).startsWith("9/3/firulais-100/").endsWith("-informe.pdf");
            // El UUID va entre el guion del slug y el guion del nombre de archivo.
            assertThat(key).matches("9/3/firulais-100/[0-9a-f-]{36}-informe\\.pdf");
        }

        @Test
        @DisplayName("dos llamadas con los mismos datos producen claves distintas: el UUID no se repite")
        void dos_llamadas_producen_claves_distintas() {
            String primera = StorageKeyFactory.build(RUTA, "informe.pdf");
            String segunda = StorageKeyFactory.build(RUTA, "informe.pdf");

            assertThat(primera).isNotEqualTo(segunda);
        }
    }

    @Nested
    @DisplayName("slug del nombre del animal")
    class Slug {

        @ParameterizedTest
        @CsvSource({"Firulais,firulais", "Michi Gato,michi-gato", "Ñoño,nono", "  Toby  ,toby"})
        @DisplayName("normaliza a minusculas, sin acentos y con guiones")
        void normaliza_el_nombre(String nombreAnimal, String slugEsperado) {
            LaboratoryTestStoragePathRef ruta = new LaboratoryTestStoragePathRef(9L, 3L, 100L,
                    nombreAnimal);

            String key = StorageKeyFactory.build(ruta, "informe.pdf");

            assertThat(key).startsWith("9/3/" + slugEsperado + "-100/");
        }

        @ParameterizedTest
        @ValueSource(strings = {"***", "???", "///"})
        @DisplayName("un nombre que se queda en blanco tras normalizar cae al literal 'animal'")
        void nombre_sin_caracteres_alfanumericos_cae_a_animal(String nombreAnimal) {
            LaboratoryTestStoragePathRef ruta = new LaboratoryTestStoragePathRef(9L, 3L, 100L,
                    nombreAnimal);

            String key = StorageKeyFactory.build(ruta, "informe.pdf");

            assertThat(key).startsWith("9/3/animal-100/");
        }
    }

    @Nested
    @DisplayName("saneamiento del nombre de archivo")
    class SaneamientoNombreArchivo {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("nulo o vacio cae al literal 'archivo'")
        void nulo_o_vacio_cae_a_archivo(String originalFileName) {
            String key = StorageKeyFactory.build(RUTA, originalFileName);

            assertThat(key).endsWith("-archivo");
        }

        @Test
        @DisplayName("en blanco tambien cae al literal 'archivo'")
        void en_blanco_cae_a_archivo() {
            String key = StorageKeyFactory.build(RUTA, "   ");

            assertThat(key).endsWith("-archivo");
        }

        @Test
        @DisplayName("los separadores de ruta se reemplazan para no escapar de la carpeta del animal")
        void separadores_de_ruta_se_reemplazan() {
            String key = StorageKeyFactory.build(RUTA, "../../etc/passwd");

            assertThat(key).endsWith("-.._.._etc_passwd").doesNotContain("/etc/passwd");
        }
    }
}
