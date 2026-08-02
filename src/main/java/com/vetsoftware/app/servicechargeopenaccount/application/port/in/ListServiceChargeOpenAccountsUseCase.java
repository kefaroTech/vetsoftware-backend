package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListServiceChargeOpenAccountsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<ServiceChargeOpenAccountDto> listAll(Long companyId);
}
