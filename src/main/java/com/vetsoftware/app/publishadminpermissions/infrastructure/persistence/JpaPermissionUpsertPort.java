package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermission;
import com.vetsoftware.app.publishadminpermissions.application.port.out.PermissionUpsertPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.UpsertedPermission;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaPermissionUpsertPort implements PermissionUpsertPort {

    private final PermissionJpaRepository permissionJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;
    private final SubModuleJpaRepository subModuleJpaRepository;

    public JpaPermissionUpsertPort(PermissionJpaRepository permissionJpaRepository,
                                   CompanyJpaRepository companyJpaRepository,
                                   SubModuleJpaRepository subModuleJpaRepository) {
        this.permissionJpaRepository = permissionJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    @Override
    public UpsertedPermission upsert(Long companyId, AdminBasePermission template) {
        Optional<PermissionJpaEntity> existing =
            permissionJpaRepository.findByCompanyIdAndCode(companyId, template.code());
        if (existing.isPresent()) {
            return new UpsertedPermission(existing.get().getId(), false);
        }
        CompanyJpaEntity companyRef = companyJpaRepository.getReferenceById(companyId);
        SubModuleJpaEntity subModuleRef = subModuleJpaRepository.getReferenceById(template.subModuleId());
        PermissionJpaEntity entity = new PermissionJpaEntity();
        entity.setName(template.name());
        entity.setCode(template.code());
        entity.setCompany(companyRef);
        entity.setSubModule(subModuleRef);
        entity.setCreatedDate(LocalDateTime.now());
        PermissionJpaEntity saved = permissionJpaRepository.save(entity);
        return new UpsertedPermission(saved.getId(), true);
    }
}
