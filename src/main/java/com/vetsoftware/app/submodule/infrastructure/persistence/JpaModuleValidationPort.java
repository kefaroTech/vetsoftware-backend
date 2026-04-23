package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaRepository;
import com.vetsoftware.app.submodule.application.port.out.ModuleValidationPort;
import org.springframework.stereotype.Component;

@Component("submoduleJpaModuleValidationPort")
public class JpaModuleValidationPort implements ModuleValidationPort {
    private final ModuleJpaRepository moduleJpaRepository;

    public JpaModuleValidationPort(ModuleJpaRepository moduleJpaRepository) {
        this.moduleJpaRepository = moduleJpaRepository;
    }

    @Override
    public void validateExists(Long moduleId) {
        if (!moduleJpaRepository.existsById(moduleId)) {
            throw new IllegalArgumentException("Module not found: " + moduleId);
        }
    }
}
