package com.vetsoftware.app.animalalert.application.port.in;

import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import com.vetsoftware.app.animalalert.application.query.ListAnimalAlertsByAnimalQuery;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAnimalAlertsByAnimalUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('animal.read') and @authz.isMyCompany(#query.companyId))")
  List<AnimalAlertDto> execute(ListAnimalAlertsByAnimalQuery query);
}
