package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAnimalsUseCase {
  @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.read'))")
  List<AnimalDto> listAll(Long companyId);
}
