package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListOpenAccountsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<OpenAccountDto> listAll();
}
