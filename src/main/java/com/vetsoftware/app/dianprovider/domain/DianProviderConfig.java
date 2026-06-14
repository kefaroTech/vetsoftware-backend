package com.vetsoftware.app.dianprovider.domain;

import java.time.LocalDateTime;

/**
 * Configuracion del proveedor DIAN de una empresa (0-o-1 por empresa). Agnostica: guarda credenciales
 * tanto OAuth2 (Factus) como token estatico (MATIAS); el adapter usa las que correspondan al provider.
 * Los secretos se cifran en la capa de persistencia (EncryptedStringConverter).
 */
public class DianProviderConfig {
    private Long id;
    private CompanyRef company;
    private ProviderType provider;
    private ProviderEnvironment environment;
    private String baseUrl;
    // Credenciales OAuth2 (Factus); null para MATIAS.
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;
    // Token estatico (MATIAS PAT); null para Factus.
    private String apiToken;
    // Verificacion de webhooks (solo proveedores async).
    private String webhookSecret;
    // Cache del token vigente.
    private String accessToken;
    private LocalDateTime tokenExpiresAt;
    private String numberingProviderRef;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public DianProviderConfig(Long id, CompanyRef company, ProviderType provider, ProviderEnvironment environment,
                              String baseUrl, String clientId, String clientSecret, String username, String password,
                              String apiToken, String webhookSecret, String accessToken, LocalDateTime tokenExpiresAt,
                              String numberingProviderRef, LocalDateTime createdDate, boolean enabled) {
        validate(company, provider, environment, baseUrl);
        this.id = id;
        this.company = company;
        this.provider = provider;
        this.environment = environment;
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

    public static DianProviderConfig create(CompanyRef company, ProviderType provider, ProviderEnvironment environment,
                                            String baseUrl, String clientId, String clientSecret, String username,
                                            String password, String apiToken, String webhookSecret,
                                            String numberingProviderRef) {
        return new DianProviderConfig(null, company, provider, environment, baseUrl, clientId, clientSecret,
                username, password, apiToken, webhookSecret, null, null, numberingProviderRef,
                LocalDateTime.now(), true);
    }

    public void update(ProviderType provider, ProviderEnvironment environment, String baseUrl, String clientId,
                       String clientSecret, String username, String password, String apiToken, String webhookSecret,
                       String numberingProviderRef) {
        validate(this.company, provider, environment, baseUrl);
        this.provider = provider;
        this.environment = environment;
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

    private static void validate(CompanyRef company, ProviderType provider, ProviderEnvironment environment,
                                 String baseUrl) {
        if (company == null) throw new IllegalArgumentException("company is required");
        if (provider == null) throw new IllegalArgumentException("provider is required");
        if (environment == null) throw new IllegalArgumentException("environment is required");
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("baseUrl is required");
        if (baseUrl.length() > 255) throw new IllegalArgumentException("baseUrl must be 255 chars or less");
    }

    public Long getId() { return id; }
    public CompanyRef getCompany() { return company; }
    public ProviderType getProvider() { return provider; }
    public ProviderEnvironment getEnvironment() { return environment; }
    public String getBaseUrl() { return baseUrl; }
    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getApiToken() { return apiToken; }
    public String getWebhookSecret() { return webhookSecret; }
    public String getAccessToken() { return accessToken; }
    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public String getNumberingProviderRef() { return numberingProviderRef; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
