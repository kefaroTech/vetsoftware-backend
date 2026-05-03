package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListBreedsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<BreedDto> listAll();
}
