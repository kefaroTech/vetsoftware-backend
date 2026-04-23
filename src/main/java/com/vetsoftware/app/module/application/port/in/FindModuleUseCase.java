package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.dto.ModuleDto;

public interface FindModuleUseCase {
    ModuleDto findById(Long id);
}
