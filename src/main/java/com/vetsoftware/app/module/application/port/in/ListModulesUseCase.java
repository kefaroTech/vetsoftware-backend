package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.dto.ModuleDto;
import java.util.List;

public interface ListModulesUseCase {
    List<ModuleDto> listAll();
}
