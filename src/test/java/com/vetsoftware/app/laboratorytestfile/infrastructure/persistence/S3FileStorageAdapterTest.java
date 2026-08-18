package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.storage.S3StorageClient;
import com.vetsoftware.app.laboratorytestfile.application.port.out.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El cliente S3 subyacente se mockea: este adaptador no habla con AWS de verdad
 * en un test unitario, solo traduce el {@link FileStoragePort} al
 * {@link S3StorageClient}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3FileStorageAdapter")
class S3FileStorageAdapterTest {

    @Mock
    private S3StorageClient s3StorageClient;

    private S3FileStorageAdapter adapter;

    @BeforeEach
    void montar() {
        adapter = new S3FileStorageAdapter(s3StorageClient);
    }

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("sube el contenido y devuelve bucket, clave y eTag del cliente S3")
        void sube_el_contenido_y_devuelve_los_datos_del_objeto() {
            byte[] content = "pdf-bytes".getBytes();
            when(s3StorageClient.putObject("9/3/firulais-100/uuid-informe.pdf", content,
                    "application/pdf")).thenReturn("etag-xyz");
            when(s3StorageClient.bucket()).thenReturn("vetsoftware-lab-files");

            FileStoragePort.StoredFile stored = adapter.store("9/3/firulais-100/uuid-informe.pdf",
                    content, "application/pdf");

            assertThat(stored.bucket()).isEqualTo("vetsoftware-lab-files");
            assertThat(stored.key()).isEqualTo("9/3/firulais-100/uuid-informe.pdf");
            assertThat(stored.eTag()).isEqualTo("etag-xyz");
        }
    }

    @Nested
    @DisplayName("retrieve")
    class Retrieve {

        @Test
        @DisplayName("delega en el cliente S3 y devuelve el contenido recibido")
        void delega_en_el_cliente_s3() {
            byte[] content = "pdf-bytes".getBytes();
            when(s3StorageClient.getObject("9/3/firulais-100/uuid-informe.pdf"))
                    .thenReturn(content);

            byte[] resultado = adapter.retrieve("9/3/firulais-100/uuid-informe.pdf");

            assertThat(resultado).isEqualTo(content);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("delega el borrado en el cliente S3 con la clave recibida")
        void delega_el_borrado_en_el_cliente_s3() {
            adapter.delete("9/3/firulais-100/uuid-informe.pdf");

            verify(s3StorageClient).deleteObject("9/3/firulais-100/uuid-informe.pdf");
        }
    }
}
