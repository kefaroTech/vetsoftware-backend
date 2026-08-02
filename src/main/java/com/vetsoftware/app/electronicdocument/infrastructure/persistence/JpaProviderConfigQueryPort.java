package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.dianprovider.infrastructure.persistence.DianProviderConfigJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Lee la config del proveedor DIAN (feature dianprovider). El converter AES descifra las
 * credenciales al leer la entidad, así que el snapshot sale ya en claro para uso del adaptador.
 */
@Component
public class JpaProviderConfigQueryPort implements ProviderConfigQueryPort {
  private final DianProviderConfigJpaRepository configJpaRepository;

  public JpaProviderConfigQueryPort(DianProviderConfigJpaRepository configJpaRepository) {
    this.configJpaRepository = configJpaRepository;
  }

  @Override
  public Optional<ProviderConfigSnapshot> findByCompanyId(Long companyId) {
    return configJpaRepository
        .findByCompany_Id(companyId)
        .map(
            e ->
                new ProviderConfigSnapshot(
                    e.getProvider().name(),
                    e.getBaseUrl(),
                    e.getClientId(),
                    e.getClientSecret(),
                    e.getUsername(),
                    e.getPassword(),
                    e.getApiToken(),
                    e.getWebhookSecret(),
                    e.getNumberingProviderRef()));
  }
}
