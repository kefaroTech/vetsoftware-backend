package com.vetsoftware.app.dianprovider.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.dianprovider.domain.ProviderType;
import com.vetsoftware.app.infrastructure.security.EncryptedStringConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "dian_provider_configs")
@SQLDelete(sql = "UPDATE dian_provider_configs SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class DianProviderConfigJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false, unique = true)
  private CompanyJpaEntity company;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private ProviderType provider;

  @Column(name = "base_url", nullable = false, length = 255)
  private String baseUrl;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "credential_client_id", length = 512)
  private String clientId;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "credential_client_secret", length = 512)
  private String clientSecret;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "credential_username", length = 512)
  private String username;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "credential_password", length = 512)
  private String password;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "api_token", length = 2000)
  private String apiToken;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "webhook_secret", length = 512)
  private String webhookSecret;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "access_token", length = 2000)
  private String accessToken;

  @Column(name = "token_expires_at")
  private LocalDateTime tokenExpiresAt;

  @Column(name = "numbering_provider_ref", length = 100)
  private String numberingProviderRef;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  protected DianProviderConfigJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CompanyJpaEntity getCompany() {
    return company;
  }

  public void setCompany(CompanyJpaEntity company) {
    this.company = company;
  }

  public ProviderType getProvider() {
    return provider;
  }

  public void setProvider(ProviderType provider) {
    this.provider = provider;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getApiToken() {
    return apiToken;
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public LocalDateTime getTokenExpiresAt() {
    return tokenExpiresAt;
  }

  public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
    this.tokenExpiresAt = tokenExpiresAt;
  }

  public String getNumberingProviderRef() {
    return numberingProviderRef;
  }

  public void setNumberingProviderRef(String numberingProviderRef) {
    this.numberingProviderRef = numberingProviderRef;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
