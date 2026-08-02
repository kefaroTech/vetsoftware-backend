package com.vetsoftware.app.dianprovider.domain;

import java.time.LocalDateTime;

/**
 * Configuracion del proveedor DIAN de una empresa (0-o-1 por empresa). Hoy el unico proveedor es
 * MATIAS, que autentica por login (username=email + password) o por PAT estatico (apiToken). El
 * esquema es agnostico por si se agregan mas proveedores. Los secretos se cifran en la capa de
 * persistencia (EncryptedStringConverter).
 */
public class DianProviderConfig {
  private Long id;
  private CompanyRef company;
  private ProviderType provider;
  private String baseUrl;
  // Credenciales de login MATIAS: username = email, password. (clientId/clientSecret reservados,
  // sin uso hoy.)
  private String clientId;
  private String clientSecret;
  private String username;
  private String password;
  // PAT estatico opcional: si esta presente el adapter lo usa y omite el login.
  private String apiToken;
  // Verificacion de webhooks (solo proveedores async).
  private String webhookSecret;
  // Cache del token vigente.
  private String accessToken;
  private LocalDateTime tokenExpiresAt;
  private String numberingProviderRef;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public DianProviderConfig(
      Long id,
      CompanyRef company,
      ProviderType provider,
      String baseUrl,
      String clientId,
      String clientSecret,
      String username,
      String password,
      String apiToken,
      String webhookSecret,
      String accessToken,
      LocalDateTime tokenExpiresAt,
      String numberingProviderRef,
      LocalDateTime createdDate,
      boolean enabled) {
    validate(company, provider, baseUrl);
    this.id = id;
    this.company = company;
    this.provider = provider;
    this.baseUrl = baseUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.username = username;
    this.password = password;
    this.apiToken = apiToken;
    this.webhookSecret = webhookSecret;
    this.accessToken = accessToken;
    this.tokenExpiresAt = tokenExpiresAt;
    this.numberingProviderRef = numberingProviderRef;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static DianProviderConfig create(
      CompanyRef company,
      ProviderType provider,
      String baseUrl,
      String clientId,
      String clientSecret,
      String username,
      String password,
      String apiToken,
      String webhookSecret,
      String numberingProviderRef) {
    return new DianProviderConfig(
        null,
        company,
        provider,
        baseUrl,
        clientId,
        clientSecret,
        username,
        password,
        apiToken,
        webhookSecret,
        null,
        null,
        numberingProviderRef,
        LocalDateTime.now(),
        true);
  }

  public void update(
      ProviderType provider,
      String baseUrl,
      String clientId,
      String clientSecret,
      String username,
      String password,
      String apiToken,
      String webhookSecret,
      String numberingProviderRef) {
    validate(this.company, provider, baseUrl);
    this.provider = provider;
    this.baseUrl = baseUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.username = username;
    this.password = password;
    this.apiToken = apiToken;
    this.webhookSecret = webhookSecret;
    this.numberingProviderRef = numberingProviderRef;
  }

  /** Guarda el token vigente devuelto por el proveedor (cache). */
  public void cacheToken(String accessToken, LocalDateTime expiresAt) {
    this.accessToken = accessToken;
    this.tokenExpiresAt = expiresAt;
  }

  private static void validate(CompanyRef company, ProviderType provider, String baseUrl) {
    if (company == null) throw new IllegalArgumentException("company is required");
    if (provider == null) throw new IllegalArgumentException("provider is required");
    if (baseUrl == null || baseUrl.isBlank())
      throw new IllegalArgumentException("baseUrl is required");
    if (baseUrl.length() > 255)
      throw new IllegalArgumentException("baseUrl must be 255 chars or less");
  }

  public Long getId() {
    return id;
  }

  public CompanyRef getCompany() {
    return company;
  }

  public ProviderType getProvider() {
    return provider;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getApiToken() {
    return apiToken;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public LocalDateTime getTokenExpiresAt() {
    return tokenExpiresAt;
  }

  public String getNumberingProviderRef() {
    return numberingProviderRef;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }
}
