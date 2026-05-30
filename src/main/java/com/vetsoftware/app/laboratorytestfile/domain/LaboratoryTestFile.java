package com.vetsoftware.app.laboratorytestfile.domain;

import java.time.LocalDateTime;

public class LaboratoryTestFile {
    private final Long id;
    private final String storageKey;
    private final String bucket;
    private final String originalFileName;
    private final String contentType;
    private final Long sizeBytes;
    private final String eTag;
    private final EmployeeRef uploadedBy;
    private final LaboratoryTestRef laboratoryTest;
    private final LocalDateTime createdDate;

    public LaboratoryTestFile(Long id, String storageKey, String bucket, String originalFileName,
                              String contentType, Long sizeBytes, String eTag, EmployeeRef uploadedBy,
                              LaboratoryTestRef laboratoryTest, LocalDateTime createdDate) {
        validate(storageKey, bucket, originalFileName, contentType, sizeBytes, eTag, uploadedBy, laboratoryTest);
        this.id = id;
        this.storageKey = storageKey;
        this.bucket = bucket;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.eTag = eTag;
        this.uploadedBy = uploadedBy;
        this.laboratoryTest = laboratoryTest;
        this.createdDate = createdDate;
    }

    public static LaboratoryTestFile create(String storageKey, String bucket, String originalFileName,
                                            String contentType, Long sizeBytes, String eTag,
                                            EmployeeRef uploadedBy, LaboratoryTestRef laboratoryTest) {
        return new LaboratoryTestFile(null, storageKey, bucket, originalFileName, contentType,
                sizeBytes, eTag, uploadedBy, laboratoryTest, LocalDateTime.now());
    }

    private static void validate(String storageKey, String bucket, String originalFileName,
                                 String contentType, Long sizeBytes, String eTag, EmployeeRef uploadedBy,
                                 LaboratoryTestRef laboratoryTest) {
        if (storageKey == null || storageKey.isBlank()) throw new IllegalArgumentException("storageKey is required");
        if (storageKey.length() > 512) throw new IllegalArgumentException("storageKey must be 512 chars or less");
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("bucket is required");
        if (originalFileName == null || originalFileName.isBlank()) throw new IllegalArgumentException("originalFileName is required");
        if (originalFileName.length() > 255) throw new IllegalArgumentException("originalFileName must be 255 chars or less");
        if (contentType == null || contentType.isBlank()) throw new IllegalArgumentException("contentType is required");
        if (sizeBytes == null) throw new IllegalArgumentException("sizeBytes is required");
        if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes cannot be negative");
        if (eTag == null || eTag.isBlank()) throw new IllegalArgumentException("eTag is required");
        if (uploadedBy == null) throw new IllegalArgumentException("uploadedBy is required");
        if (laboratoryTest == null) throw new IllegalArgumentException("laboratoryTest is required");
    }

    public Long getId() { return id; }
    public String getStorageKey() { return storageKey; }
    public String getBucket() { return bucket; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getETag() { return eTag; }
    public EmployeeRef getUploadedBy() { return uploadedBy; }
    public LaboratoryTestRef getLaboratoryTest() { return laboratoryTest; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
