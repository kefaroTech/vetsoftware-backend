package com.vetsoftware.app.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Excepcion de dominio simple: solo constructor + mensaje + causa. Sin mocks,
 * JUnit puro.
 */
@DisplayName("S3StorageException")
class S3StorageExceptionTest {

    @Nested
    @DisplayName("solo mensaje")
    class SoloMensaje {

        @Test
        @DisplayName("expone el mensaje y no tiene causa")
        void expone_el_mensaje_y_no_tiene_causa() {
            S3StorageException ex = new S3StorageException("Failed to upload object to S3: k1");

            assertThat(ex).hasMessage("Failed to upload object to S3: k1").hasNoCause()
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("mensaje y causa")
    class MensajeYCausa {

        @Test
        @DisplayName("expone el mensaje y conserva la causa original")
        void expone_el_mensaje_y_conserva_la_causa() {
            RuntimeException causaOriginal = new RuntimeException("S3 no responde");

            S3StorageException ex = new S3StorageException("Failed to download object from S3: k2",
                    causaOriginal);

            assertThat(ex).hasMessage("Failed to download object from S3: k2")
                    .hasCause(causaOriginal);
        }
    }
}
