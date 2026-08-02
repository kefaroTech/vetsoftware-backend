package com.vetsoftware.app.basepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.application.port.out.MandatoryBaseRolePermissionInitializationPort;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class JpaMandatoryBaseRolePermissionInitializationPort
        implements
            MandatoryBaseRolePermissionInitializationPort {
    private final BaseRoleJpaRepository baseRoleJpaRepository;
    private final BasePermissionJpaRepository basePermissionJpaRepository;
    private final BaseRolePermissionJpaRepository baseRolePermissionJpaRepository;

    public JpaMandatoryBaseRolePermissionInitializationPort(
            BaseRoleJpaRepository baseRoleJpaRepository,
            BasePermissionJpaRepository basePermissionJpaRepository,
            BaseRolePermissionJpaRepository baseRolePermissionJpaRepository) {
        this.baseRoleJpaRepository = baseRoleJpaRepository;
        this.basePermissionJpaRepository = basePermissionJpaRepository;
        this.baseRolePermissionJpaRepository = baseRolePermissionJpaRepository;
    }

    @Override
    public void initializeForMandatoryBaseRoles(Long basePermissionId) {
        var basePermission = basePermissionJpaRepository.getReferenceById(basePermissionId);
        var entities = baseRoleJpaRepository.findByMandatoryTrue().stream()
                .filter(br -> !baseRolePermissionJpaRepository
                        .existsByBaseRoleIdAndBasePermissionId(br.getId(), basePermissionId))
                .map(br -> {
                    var entity = new BaseRolePermissionJpaEntity();
                    entity.setBaseRole(br);
                    entity.setBasePermission(basePermission);
                    entity.setCreatedDate(LocalDateTime.now());
                    return entity;
                }).toList();
        baseRolePermissionJpaRepository.saveAll(entities);
    }
}
