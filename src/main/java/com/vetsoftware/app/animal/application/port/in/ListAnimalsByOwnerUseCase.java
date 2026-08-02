package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAnimalsByOwnerUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.read') and @authz.isMyCompany(#companyId))")
    List<AnimalDto> listByOwner(Long ownerId, Long companyId);
}
