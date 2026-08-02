package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListVaccinationTypesUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('vaccination.read')")
  List<VaccinationTypeDto> listAll();
}
