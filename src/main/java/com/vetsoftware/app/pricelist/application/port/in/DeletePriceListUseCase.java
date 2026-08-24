package com.vetsoftware.app.pricelist.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeletePriceListUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    void execute(Long id);
}
