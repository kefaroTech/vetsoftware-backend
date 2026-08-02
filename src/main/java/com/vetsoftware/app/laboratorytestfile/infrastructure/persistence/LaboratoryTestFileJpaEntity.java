package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "laboratory_test_files")
public class LaboratoryTestFileJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "storage_key", nullable = false, length = 512)
  private String storageKey;

  @Column(nullable = false, length = 255)
  private String bucket;

  @Column(name = "original_file_name", nullable = false, length = 255)
  private String originalFileName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "e_tag", nullable = false, length = 255)
  private String eTag;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "uploaded_by_id", nullable = false)
  private EmployeeJpaEntity uploadedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "laboratory_test_id", nullable = false)
  private LaboratoryTestJpaEntity laboratoryTest;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  protected LaboratoryTestFileJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public void setStorageKey(String storageKey) {
    this.storageKey = storageKey;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getOriginalFileName() {
    return originalFileName;
  }

  public void setOriginalFileName(String originalFileName) {
    this.originalFileName = originalFileName;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(Long sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public String getETag() {
    return eTag;
  }

  public void setETag(String eTag) {
    this.eTag = eTag;
  }

  public EmployeeJpaEntity getUploadedBy() {
    return uploadedBy;
  }

  public void setUploadedBy(EmployeeJpaEntity uploadedBy) {
    this.uploadedBy = uploadedBy;
  }

  public LaboratoryTestJpaEntity getLaboratoryTest() {
    return laboratoryTest;
  }

  public void setLaboratoryTest(LaboratoryTestJpaEntity laboratoryTest) {
    this.laboratoryTest = laboratoryTest;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }
}
