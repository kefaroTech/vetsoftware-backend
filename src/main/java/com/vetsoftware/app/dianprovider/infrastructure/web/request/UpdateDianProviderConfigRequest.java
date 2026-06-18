package com.vetsoftware.app.dianprovider.infrastructure.web.request;

import com.vetsoftware.app.dianprovider.domain.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateDianProviderConfigRequest(
        @NotNull ProviderType provider,
        @NotBlank String baseUrl,
        String clientId,
        String clientSecret,
        String username,
        String password,
        String apiToken,
        String webhookSecret,
        String numberingProviderRef
) {}
