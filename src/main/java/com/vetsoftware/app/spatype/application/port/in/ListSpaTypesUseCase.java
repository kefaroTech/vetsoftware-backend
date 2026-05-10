package com.vetsoftware.app.spatype.application.port.in;

import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSpaTypesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<SpaTypeDto> listAll();
}
