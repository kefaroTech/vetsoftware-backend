package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSurgeriesUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('surgery.read')")
  List<SurgeryDto> listAll();
}
