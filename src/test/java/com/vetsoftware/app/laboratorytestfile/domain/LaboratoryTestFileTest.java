package com.vetsoftware.app.laboratorytestfile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.laboratorytestfile.testsupport.LaboratoryTestFileMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("LaboratoryTestFile — invariantes del archivo de examen de laboratorio")
class LaboratoryTestFileTest {

    private static final EmployeeRef VETERINARIO = LaboratoryTestFileMother.VETERINARIO;
    private static final LaboratoryTestRef EXAMEN = LaboratoryTestFileMother.EXAMEN;

    private static LaboratoryTestFile construir(String storageKey, String bucket,
            String originalFileName, String contentType, Long sizeBytes, String eTag,
            EmployeeRef uploadedBy, LaboratoryTestRef laboratoryTest) {
        return new LaboratoryTestFile(LaboratoryTestFileMother.FILE_ID, storageKey, bucket,
                originalFileName, contentType, sizeBytes, eTag, uploadedBy, laboratoryTest,
                LaboratoryTestFileMother.CREADO);
    }

    @Nested
    @DisplayName("construccion valida")
    class ConstruccionValida {

        @Test
        @DisplayName("conserva cada campo tal como se paso")
        void conserva_cada_campo() {
            LaboratoryTestFile file = LaboratoryTestFileMother.archivoValido();

            assertThat(file.getId()).isEqualTo(LaboratoryTestFileMother.FILE_ID);
            assertThat(file.getStorageKey()).isEqualTo(LaboratoryTestFileMother.STORAGE_KEY);
            assertThat(file.getBucket()).isEqualTo(LaboratoryTestFileMother.BUCKET);
            assertThat(file.getOriginalFileName())
                    .isEqualTo(LaboratoryTestFileMother.ORIGINAL_FILE_NAME);
            assertThat(file.getContentType()).isEqualTo(LaboratoryTestFileMother.CONTENT_TYPE);
            assertThat(file.getSizeBytes()).isEqualTo(LaboratoryTestFileMother.SIZE_BYTES);
            assertThat(file.getETag()).isEqualTo(LaboratoryTestFileMother.E_TAG);
            assertThat(file.getUploadedBy()).isEqualTo(VETERINARIO);
            assertThat(file.getLaboratoryTest()).isEqualTo(EXAMEN);
            assertThat(file.getCreatedDate()).isEqualTo(LaboratoryTestFileMother.CREADO);
        }

        @Test
        @DisplayName("sizeBytes en cero es un archivo valido, no negativo")
        void sizeBytes_en_cero_es_valido() {
            LaboratoryTestFile file = construir("key", "bucket", "vacio.txt", "text/plain", 0L,
                    "etag", VETERINARIO, EXAMEN);

            assertThat(file.getSizeBytes()).isZero();
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("crea sin id todavia y con la fecha de creacion asignada")
        void crea_sin_id_con_fecha_asignada() {
            LaboratoryTestFile file = LaboratoryTestFile.create("key", "bucket", "informe.pdf",
                    "application/pdf", 100L, "etag", VETERINARIO, EXAMEN);

            assertThat(file.getId()).isNull();
            assertThat(file.getStorageKey()).isEqualTo("key");
            assertThat(file.getCreatedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("invariantes de storageKey")
    class InvariantesStorageKey {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("nulo o en blanco se rechaza")
        void nulo_o_en_blanco_se_rechaza(String valor) {
            assertThatThrownBy(() -> construir(valor, "bucket", "informe.pdf", "application/pdf",
                    100L, "etag", VETERINARIO, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("storageKey is required");
        }

        @Test
        @DisplayName("de mas de 512 caracteres se rechaza")
        void demasiado_largo_se_rechaza() {
            String largo = "a".repeat(513);

            assertThatThrownBy(() -> construir(largo, "bucket", "informe.pdf", "application/pdf",
                    100L, "etag", VETERINARIO, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("storageKey must be 512 chars or less");
        }

        @Test
        @DisplayName("de exactamente 512 caracteres se acepta")
        void exactamente_512_se_acepta() {
            String limite = "a".repeat(512);

            LaboratoryTestFile file = construir(limite, "bucket", "informe.pdf", "application/pdf",
                    100L, "etag", VETERINARIO, EXAMEN);

            assertThat(file.getStorageKey()).hasSize(512);
        }
    }

    @Nested
    @DisplayName("invariantes de bucket")
    class InvariantesBucket {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("nulo o en blanco se rechaza")
        void nulo_o_en_blanco_se_rechaza(String valor) {
            assertThatThrownBy(() -> construir("key", valor, "informe.pdf", "application/pdf", 100L,
                    "etag", VETERINARIO, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bucket is required");
        }
    }

    @Nested
    @DisplayName("invariantes de originalFileName")
    class InvariantesOriginalFileName {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("nulo o en blanco se rechaza")
        void nulo_o_en_blanco_se_rechaza(String valor) {
            assertThatThrownBy(() -> construir("key", "bucket", valor, "application/pdf", 100L,
                    "etag", VETERINARIO, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("originalFileName is required");
        }

        @Test
        @DisplayName("de mas de 255 caracteres se rechaza")
        void demasiado_largo_se_rechaza() {
            String largo = "a".repeat(256);

            assertThatThrownBy(() -> construir("key", "bucket", largo, "application/pdf", 100L,
                    "etag", VETERINARIO, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("originalFileName must be 255 chars or less");
        }

        @Test
        @DisplayName("de exactamente 255 caracteres se acepta")
        void exactamente_255_se_acepta() {
            String limite = "a".repeat(255);

            LaboratoryTestFile file = construir("key", "bucket", limite, "application/pdf", 100L,
                    "etag", VETERINARIO, EXAMEN);

            assertThat(file.getOriginalFileName()).hasSize(255);
        }
    }

    @Nested
    @DisplayName("invariantes de contentType")
    class InvariantesContentType {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("nulo o en blanco se rechaza")
        void nulo_o_en_blanco_se_rechaza(String valor) {
            assertThatThrownBy(() -> construir("key", "bucket", "informe.pdf", valor, 100L, "etag",
                    VETERINARIO, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contentType is required");
        }
    }

    @Nested
    @DisplayName("invariantes de sizeBytes")
    class InvariantesSizeBytes {

        @Test
        @DisplayName("nulo se rechaza")
        void nulo_se_rechaza() {
            assertThatThrownBy(() -> construir("key", "bucket", "informe.pdf", "application/pdf",
                    null, "etag", VETERINARIO, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sizeBytes is required");
        }

        @Test
        @DisplayName("negativo se rechaza")
        void negativo_se_rechaza() {
            assertThatThrownBy(() -> construir("key", "bucket", "informe.pdf", "application/pdf",
                    -1L, "etag", VETERINARIO, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sizeBytes cannot be negative");
        }
    }

    @Nested
    @DisplayName("invariantes de eTag")
    class InvariantesETag {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("nulo o en blanco se rechaza")
        void nulo_o_en_blanco_se_rechaza(String valor) {
            assertThatThrownBy(() -> construir("key", "bucket", "informe.pdf", "application/pdf",
                    100L, valor, VETERINARIO, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eTag is required");
        }
    }

    @Nested
    @DisplayName("invariantes de las referencias")
    class InvariantesReferencias {

        @Test
        @DisplayName("uploadedBy nulo se rechaza")
        void uploadedBy_nulo_se_rechaza() {
            assertThatThrownBy(() -> construir("key", "bucket", "informe.pdf", "application/pdf",
                    100L, "etag", null, EXAMEN)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("uploadedBy is required");
        }

        @Test
        @DisplayName("laboratoryTest nulo se rechaza")
        void laboratoryTest_nulo_se_rechaza() {
            assertThatThrownBy(() -> construir("key", "bucket", "informe.pdf", "application/pdf",
                    100L, "etag", VETERINARIO, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("laboratoryTest is required");
        }
    }
}
