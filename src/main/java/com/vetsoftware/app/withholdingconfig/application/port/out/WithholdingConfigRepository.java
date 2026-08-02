package com.vetsoftware.app.withholdingconfig.application.port.out;

import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfig;
import java.util.Optional;

public interface WithholdingConfigRepository {
    WithholdingConfig save(WithholdingConfig config);

    Optional<WithholdingConfig> findByCompanyId(Long companyId);
}
