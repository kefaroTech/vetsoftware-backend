package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.dto.ModuleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListModulesUseCase {
    List<ModuleDto> listAll();
}
