package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDayCaresUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('dayCare.read')")
  List<DayCareDto> listAll();
}
