package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.specie.application.dto.SpecieDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSpeciesUseCase {
    List<SpecieDto> listAll();
}
