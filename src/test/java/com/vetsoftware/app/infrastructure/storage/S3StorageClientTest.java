package com.vetsoftware.app.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * El unico puerto real aqui es el {@link S3Client} del SDK: se mockea. El
 * {@link S3Properties} es un record de configuracion, se construye de verdad.
 * Nunca se llama a AWS real ni a LocalStack/Testcontainers.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3StorageClient")
class S3StorageClientTest {

    private static final String BUCKET = "vetsoftware-files";

    @Mock
    private S3Client s3Client;

    private S3StorageClient client;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties(BUCKET, "us-east-1", null, null, null, false);
        client = new S3StorageClient(s3Client, properties);
    }

    @Nested
    @DisplayName("bucket configurado")
    class Bucket {

        @Test
        @DisplayName("expone el bucket recibido de las propiedades")
        void expone_el_bucket_recibido_de_las_propiedades() {
            assertThat(client.bucket()).isEqualTo(BUCKET);
        }
    }

    @Nested
    @DisplayName("putObject")
    class PutObject {

        @Test
        @DisplayName("camino feliz: sube el contenido con bucket/key/contentType y devuelve el eTag")
        void sube_el_contenido_y_devuelve_el_etag() {
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().eTag("\"abc123\"").build());

            String eTag = client.putObject("docs/1/factura.pdf", "contenido".getBytes(),
                    "application/pdf");

            ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor
                    .forClass(PutObjectRequest.class);
            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
            PutObjectRequest sent = captor.getValue();
            assertThat(sent.bucket()).isEqualTo(BUCKET);
            assertThat(sent.key()).isEqualTo("docs/1/factura.pdf");
            assertThat(sent.contentType()).isEqualTo("application/pdf");
            assertThat(eTag).isEqualTo("\"abc123\"");
        }

        @Test
        @DisplayName("un S3Exception del SDK se envuelve en S3StorageException con la clave en el mensaje")
        void un_s3exception_se_envuelve_en_s3storage_exception() {
            S3Exception fallo = (S3Exception) S3Exception.builder().message("Access Denied")
                    .statusCode(403).build();
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(fallo);

            assertThatThrownBy(
                    () -> client.putObject("docs/1/factura.pdf", "x".getBytes(), "text/plain"))
                    .isInstanceOf(S3StorageException.class)
                    .hasMessageContaining("Failed to upload object to S3: docs/1/factura.pdf")
                    .hasCause(fallo);
        }
    }

    @Nested
    @DisplayName("getObject")
    class GetObject {

        @Test
        @DisplayName("camino feliz: descarga y devuelve el contenido por su clave")
        void descarga_el_contenido_por_su_clave() {
            byte[] contenido = "contenido-original".getBytes();
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(
                    ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), contenido));

            byte[] resultado = client.getObject("docs/1/factura.pdf");

            ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor
                    .forClass(GetObjectRequest.class);
            verify(s3Client).getObjectAsBytes(captor.capture());
            assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
            assertThat(captor.getValue().key()).isEqualTo("docs/1/factura.pdf");
            assertThat(resultado).isEqualTo(contenido);
        }

        @Test
        @DisplayName("un S3Exception del SDK se envuelve en S3StorageException con la clave en el mensaje")
        void un_s3exception_se_envuelve_en_s3storage_exception() {
            S3Exception fallo = (S3Exception) S3Exception.builder().message("Not Found")
                    .statusCode(404).build();
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(fallo);

            assertThatThrownBy(() -> client.getObject("docs/1/no-existe.pdf"))
                    .isInstanceOf(S3StorageException.class)
                    .hasMessageContaining("Failed to download object from S3: docs/1/no-existe.pdf")
                    .hasCause(fallo);
        }
    }

    @Nested
    @DisplayName("deleteObject")
    class DeleteObject {

        @Test
        @DisplayName("camino feliz: borra por bucket/key y no propaga nada")
        void borra_por_bucket_y_clave() {
            client.deleteObject("docs/1/factura.pdf");

            ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor
                    .forClass(DeleteObjectRequest.class);
            verify(s3Client).deleteObject(captor.capture());
            assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
            assertThat(captor.getValue().key()).isEqualTo("docs/1/factura.pdf");
            verifyNoMoreInteractions(s3Client);
        }

        @Test
        @DisplayName("un S3Exception del SDK se envuelve en S3StorageException con la clave en el mensaje")
        void un_s3exception_se_envuelve_en_s3storage_exception() {
            S3Exception fallo = (S3Exception) S3Exception.builder().message("Service Unavailable")
                    .statusCode(503).build();
            when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(fallo);

            assertThatThrownBy(() -> client.deleteObject("docs/1/factura.pdf"))
                    .isInstanceOf(S3StorageException.class)
                    .hasMessageContaining("Failed to delete object from S3: docs/1/factura.pdf")
                    .hasCause(fallo);
        }
    }
}
