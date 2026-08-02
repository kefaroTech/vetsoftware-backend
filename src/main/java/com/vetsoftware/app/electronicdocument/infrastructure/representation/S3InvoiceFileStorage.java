package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceFileStoragePort;
import com.vetsoftware.app.infrastructure.storage.S3StorageClient;
import org.springframework.stereotype.Component;

/**
 * Guarda el PDF/XML de la factura en S3 y devuelve la clave como referencia persistida en el
 * documento.
 */
@Component
public class S3InvoiceFileStorage implements InvoiceFileStoragePort {

  private final S3StorageClient s3StorageClient;

  public S3InvoiceFileStorage(S3StorageClient s3StorageClient) {
    this.s3StorageClient = s3StorageClient;
  }

  @Override
  public String store(String key, byte[] content, String contentType) {
    s3StorageClient.putObject(key, content, contentType);
    return key;
  }
}
