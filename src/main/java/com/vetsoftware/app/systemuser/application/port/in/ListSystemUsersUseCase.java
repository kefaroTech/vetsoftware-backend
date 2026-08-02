package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSystemUsersUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<SystemUserDto> listAll();
}
