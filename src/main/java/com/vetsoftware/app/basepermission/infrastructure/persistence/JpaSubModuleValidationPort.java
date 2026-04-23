package com.vetsoftware.app.basepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import org.springframework.stereotype.Component;

@Component
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
