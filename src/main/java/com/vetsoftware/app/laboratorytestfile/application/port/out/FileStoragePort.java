package com.vetsoftware.app.laboratorytestfile.application.port.out;

public interface FileStoragePort {

    StoredFile store(String key, byte[] content, String contentType);

    byte[] retrieve(String key);

    void delete(String key);

    record StoredFile(String bucket, String key, String eTag) {
    }
}
