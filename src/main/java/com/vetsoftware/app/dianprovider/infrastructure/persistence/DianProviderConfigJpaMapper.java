package com.vetsoftware.app.dianprovider.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.dianprovider.domain.CompanyRef;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import org.springframework.stereotype.Component;

@Component
public class DianProviderConfigJpaMapper {

    public DianProviderConfigJpaEntity toJpa(DianProviderConfig config, CompanyJpaEntity company) {
        DianProviderConfigJpaEntity entity = new DianProviderConfigJpaEntity();
        entity.setId(config.getId());
        entity.setCompany(company);
        entity.setProvider(config.getProvider());
        entity.setBaseUrl(config.getBaseUrl());
        entity.setClientId(config.getClientId());
        entity.setClientSecret(config.getClientSecret());
        entity.setUsername(config.getUsername());
        entity.setPassword(config.getPassword());
        entity.setApiToken(config.getApiToken());
        entity.setWebhookSecret(config.getWebhookSecret());
        entity.setAccessToken(config.getAccessToken());
        entity.setTokenExpiresAt(config.getTokenExpiresAt());
        entity.setNumberingProviderRef(config.getNumberingProviderRef());
        entity.setCreatedDate(config.getCreatedDate());
        entity.setEnabled(config.isEnabled());
        return entity;
    }

    public DianProviderConfig toDomain(DianProviderConfigJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        CompanyRef companyRef = c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier());
        return toDomain(entity, companyRef);
    }

    public DianProviderConfig toDomain(DianProviderConfigJpaEntity entity, CompanyRef companyRef) {
        return new DianProviderConfig(
                entity.getId(), companyRef, entity.getProvider(), entity.getBaseUrl(),
                entity.getClientId(), entity.getClientSecret(), entity.getUsername(), entity.getPassword(),
                entity.getApiToken(), entity.getWebhookSecret(), entity.getAccessToken(),
                entity.getTokenExpiresAt(), entity.getNumberingProviderRef(), entity.getCreatedDate(),
                entity.isEnabled());
    }
}
