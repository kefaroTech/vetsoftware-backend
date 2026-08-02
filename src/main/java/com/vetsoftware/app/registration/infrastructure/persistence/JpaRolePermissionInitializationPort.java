package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.registration.application.port.out.RolePermissionInitializationPort;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaEntity;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JpaRolePermissionInitializationPort implements RolePermissionInitializationPort {

  private final MembershipSubModuleJpaRepository membershipSubModuleJpaRepository;
  private final BaseRolePermissionJpaRepository baseRolePermissionJpaRepository;
  private final PermissionJpaRepository permissionJpaRepository;
  private final RolePermissionJpaRepository rolePermissionJpaRepository;
  private final CompanyJpaRepository companyJpaRepository;
  private final RoleJpaRepository roleJpaRepository;

  public JpaRolePermissionInitializationPort(
      MembershipSubModuleJpaRepository membershipSubModuleJpaRepository,
      BaseRolePermissionJpaRepository baseRolePermissionJpaRepository,
      PermissionJpaRepository permissionJpaRepository,
      RolePermissionJpaRepository rolePermissionJpaRepository,
      CompanyJpaRepository companyJpaRepository,
      RoleJpaRepository roleJpaRepository) {
    this.membershipSubModuleJpaRepository = membershipSubModuleJpaRepository;
    this.baseRolePermissionJpaRepository = baseRolePermissionJpaRepository;
    this.permissionJpaRepository = permissionJpaRepository;
    this.rolePermissionJpaRepository = rolePermissionJpaRepository;
    this.companyJpaRepository = companyJpaRepository;
    this.roleJpaRepository = roleJpaRepository;
  }

  @Override
  public void initializeForRole(Long roleId, Long companyId, Long baseRoleId, Long membershipId) {
    Set<Long> subModuleIds =
        membershipSubModuleJpaRepository.findByMembershipId(membershipId).stream()
            .map(msm -> msm.getSubModule().getId())
            .collect(Collectors.toSet());
    if (subModuleIds.isEmpty()) return;

    List<BasePermissionJpaEntity> applicableBasePermissions =
        baseRolePermissionJpaRepository.findByBaseRoleId(baseRoleId).stream()
            .map(BaseRolePermissionJpaEntity::getBasePermission)
            .filter(bp -> subModuleIds.contains(bp.getSubModule().getId()))
            .toList();
    if (applicableBasePermissions.isEmpty()) return;

    CompanyJpaEntity companyRef = companyJpaRepository.getReferenceById(companyId);
    List<PermissionJpaEntity> resolvedPermissions =
        new ArrayList<>(applicableBasePermissions.size());
    List<PermissionJpaEntity> permissionsToCreate = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (BasePermissionJpaEntity bp : applicableBasePermissions) {
      PermissionJpaEntity permission =
          permissionJpaRepository
              .findByCompanyIdAndCode(companyId, bp.getCode())
              .orElseGet(
                  () -> {
                    PermissionJpaEntity entity = new PermissionJpaEntity();
                    entity.setName(bp.getName());
                    entity.setCode(bp.getCode());
                    entity.setCompany(companyRef);
                    entity.setSubModule(bp.getSubModule());
                    entity.setCreatedDate(now);
                    permissionsToCreate.add(entity);
                    return entity;
                  });
      resolvedPermissions.add(permission);
    }

    if (!permissionsToCreate.isEmpty()) {
      permissionJpaRepository.saveAll(permissionsToCreate);
    }

    RoleJpaEntity roleRef = roleJpaRepository.getReferenceById(roleId);
    List<RolePermissionJpaEntity> rolePermissions =
        resolvedPermissions.stream()
            .map(
                p -> {
                  RolePermissionJpaEntity rp = new RolePermissionJpaEntity();
                  rp.setRole(roleRef);
                  rp.setPermission(p);
                  rp.setCreatedDate(now);
                  return rp;
                })
            .toList();
    rolePermissionJpaRepository.saveAll(rolePermissions);
  }
}
