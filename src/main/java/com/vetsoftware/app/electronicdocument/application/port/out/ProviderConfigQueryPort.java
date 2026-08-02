package com.vetsoftware.app.electronicdocument.application.port.out;

import java.util.Optional;

/** Lee la config del proveedor DIAN de una empresa (descifrada) desde la feature dianprovider. */
public interface ProviderConfigQueryPort {
  Optional<ProviderConfigSnapshot> findByCompanyId(Long companyId);
}
