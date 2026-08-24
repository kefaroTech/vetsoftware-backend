package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ArchivePriceListUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PriceListDto execute(Long id);
}
