package com.vetsoftware.app.dianprovider.application.port.out;

import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import java.util.Optional;

public interface DianProviderConfigRepository {
    DianProviderConfig save(DianProviderConfig config);
    Optional<DianProviderConfig> findByCompanyId(Long companyId);
}
