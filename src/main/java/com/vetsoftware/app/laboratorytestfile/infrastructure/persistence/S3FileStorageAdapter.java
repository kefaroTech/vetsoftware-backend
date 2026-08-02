package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import com.vetsoftware.app.infrastructure.storage.S3StorageClient;
import com.vetsoftware.app.laboratorytestfile.application.port.out.FileStoragePort;
import org.springframework.stereotype.Component;

@Component
public class S3FileStorageAdapter implements FileStoragePort {
  private final S3StorageClient s3StorageClient;

  public S3FileStorageAdapter(S3StorageClient s3StorageClient) {
    this.s3StorageClient = s3StorageClient;
  }

  @Override
  public StoredFile store(String key, byte[] content, String contentType) {
    String eTag = s3StorageClient.putObject(key, content, contentType);
    return new StoredFile(s3StorageClient.bucket(), key, eTag);
  }

  @Override
  public byte[] retrieve(String key) {
    return s3StorageClient.getObject(key);
  }

  @Override
  public void delete(String key) {
    s3StorageClient.deleteObject(key);
  }
}
