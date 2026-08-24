package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListConfiguratorEffectsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<ConfiguratorEffectDto> listAll(int page, int pageSize);
}
