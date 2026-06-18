package com.vetsoftware.app.dianprovider.application.command;

import com.vetsoftware.app.dianprovider.domain.ProviderType;

public record UpdateDianProviderConfigCommand(
        ProviderType provider,
        String baseUrl,
        String clientId,
        String clientSecret,
        String username,
        String password,
        String apiToken,
        String webhookSecret,
        String numberingProviderRef,
        Long companyId
) {}
