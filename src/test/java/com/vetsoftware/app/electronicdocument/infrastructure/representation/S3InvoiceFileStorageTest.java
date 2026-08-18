package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.infrastructure.storage.S3StorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3InvoiceFileStorage — guarda el archivo en S3 y devuelve su clave")
class S3InvoiceFileStorageTest {

    @Mock
    private S3StorageClient s3StorageClient;

    private S3InvoiceFileStorage storage;

    @BeforeEach
    void montar() {
        storage = new S3InvoiceFileStorage(s3StorageClient);
    }

    @Test
    @DisplayName("delega en el cliente S3 y devuelve la clave recibida, no lo que devuelva S3")
    void delega_en_s3_y_devuelve_la_clave_recibida() {
        byte[] content = "pdf-bytes".getBytes();

        String result = storage.store("invoices/9/1/SETP990.pdf", content, "application/pdf");

        assertThat(result).isEqualTo("invoices/9/1/SETP990.pdf");
        verify(s3StorageClient).putObject("invoices/9/1/SETP990.pdf", content, "application/pdf");
    }
}
