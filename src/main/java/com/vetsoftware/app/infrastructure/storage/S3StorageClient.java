package com.vetsoftware.app.infrastructure.storage;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class S3StorageClient {

    private final S3Client s3Client;
    private final String bucket;

    public S3StorageClient(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.bucket();
    }

    /**
     * Crea (o reemplaza) un archivo en S3 bajo la clave indicada. Devuelve el eTag
     * asignado por S3.
     */
    public String putObject(String key, byte[] content, String contentType) {
        try {
            PutObjectResponse response = s3Client
                    .putObject(
                            PutObjectRequest.builder().bucket(bucket).key(key)
                                    .contentType(contentType).build(),
                            RequestBody.fromBytes(content));
            return response.eTag();
        } catch (S3Exception e) {
            throw new S3StorageException("Failed to upload object to S3: " + key, e);
        }
    }

    public byte[] getObject(String key) {
        try {
            ResponseBytes<GetObjectResponse> response = s3Client
                    .getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
            return response.asByteArray();
        } catch (S3Exception e) {
            throw new S3StorageException("Failed to download object from S3: " + key, e);
        }
    }

    /** Elimina un archivo de S3 por su clave (idempotente). */
    public void deleteObject(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw new S3StorageException("Failed to delete object from S3: " + key, e);
        }
    }

    public String bucket() {
        return bucket;
    }
}
