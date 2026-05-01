package com.vetsoftware.app.permission.infrastructure.persistence;

import com.vetsoftware.app.permission.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import org.springframework.stereotype.Component;

@Component("permissionJpaSubModuleValidationPort")
public class JpaSubModuleValidationPort implements SubModuleValidationPort {
    private final SubModuleJpaRepository subModuleJpaRepository;

    public JpaSubModuleValidationPort(SubModuleJpaRepository subModuleJpaRepository) {
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    @Override
    public void validateExists(Long subModuleId) {
        if (!subModuleJpaRepository.existsById(subModuleId)) {
            throw new IllegalArgumentException("SubModule not found: " + subModuleId);
        }
    }
}
