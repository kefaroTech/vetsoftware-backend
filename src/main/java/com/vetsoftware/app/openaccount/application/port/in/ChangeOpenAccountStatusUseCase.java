package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ChangeOpenAccountStatusUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('openAccount.update') or hasRole('SYSTEM')")
    OpenAccountDto execute(ChangeOpenAccountStatusCommand command);
}
