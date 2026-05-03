package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDewormingsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<DewormingDto> listAll();
}
