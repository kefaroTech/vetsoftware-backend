package com.vetsoftware.app.dianprovider.application.dto;

import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import com.vetsoftware.app.dianprovider.domain.ProviderType;
import java.time.LocalDateTime;

/**
 * Salida de la config. NUNCA expone secretos en claro: las credenciales se reportan como booleanos
 * "configurado" (clientId visible por ser semi-publico; secret/password/token/webhookSecret se enmascaran).
 */
public record DianProviderConfigDto(
        Long id,
        Long companyId,
        ProviderType provider,
        String baseUrl,
        String clientId,
        boolean clientSecretConfigured,
        boolean usernameConfigured,
        boolean passwordConfigured,
        boolean apiTokenConfigured,
        boolean webhookSecretConfigured,
        String numberingProviderRef,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static DianProviderConfigDto from(DianProviderConfig c) {
        return new DianProviderConfigDto(
                c.getId(),
                c.getCompany() == null ? null : c.getCompany().id(),
                c.getProvider(),
                c.getBaseUrl(),
                c.getClientId(),
                isSet(c.getClientSecret()),
                isSet(c.getUsername()),
                isSet(c.getPassword()),
                isSet(c.getApiToken()),
                isSet(c.getWebhookSecret()),
                c.getNumberingProviderRef(),
                c.getCreatedDate(),
                c.isEnabled());
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
