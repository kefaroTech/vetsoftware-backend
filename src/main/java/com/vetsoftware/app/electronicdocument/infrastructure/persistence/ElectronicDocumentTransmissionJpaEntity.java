package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "electronic_document_transmissions")
public class ElectronicDocumentTransmissionJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "electronic_document_id", nullable = false)
  private Long electronicDocumentId;

  @Column(name = "provider", nullable = false, length = 20)
  private String provider;

  @Column(name = "attempt", nullable = false)
  private int attempt;

  @Column(name = "http_status")
  private Integer httpStatus;

  @Column(name = "provider_document_key", length = 255)
  private String providerDocumentKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "result", nullable = false, length = 20)
  private TransmissionResult result;

  @Column(name = "error_message", length = 2000)
  private String errorMessage;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  protected ElectronicDocumentTransmissionJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getElectronicDocumentId() {
    return electronicDocumentId;
  }

  public void setElectronicDocumentId(Long electronicDocumentId) {
    this.electronicDocumentId = electronicDocumentId;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public int getAttempt() {
    return attempt;
  }

  public void setAttempt(int attempt) {
    this.attempt = attempt;
  }

  public Integer getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(Integer httpStatus) {
    this.httpStatus = httpStatus;
  }

  public String getProviderDocumentKey() {
    return providerDocumentKey;
  }

  public void setProviderDocumentKey(String providerDocumentKey) {
    this.providerDocumentKey = providerDocumentKey;
  }

  public TransmissionResult getResult() {
    return result;
  }

  public void setResult(TransmissionResult result) {
    this.result = result;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }
}
