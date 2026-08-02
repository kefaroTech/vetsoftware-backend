package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository.DisabledRolePermissionLookup;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRolePermissionRepository implements RolePermissionRepository {
  private final RolePermissionJpaRepository jpaRepository;
  private final RolePermissionJpaMapper mapper;
  private final RoleJpaRepository roleJpaRepository;
  private final PermissionJpaRepository permissionJpaRepository;

  public JpaRolePermissionRepository(
      RolePermissionJpaRepository jpaRepository,
      RolePermissionJpaMapper mapper,
      RoleJpaRepository roleJpaRepository,
      PermissionJpaRepository permissionJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.roleJpaRepository = roleJpaRepository;
    this.permissionJpaRepository = permissionJpaRepository;
  }

  @Override
  public RolePermission save(RolePermission rolePermission) {
    RoleJpaEntity role = roleJpaRepository.getReferenceById(rolePermission.getRole().id());
    PermissionJpaEntity permission =
        permissionJpaRepository.getReferenceById(rolePermission.getPermission().id());
    RolePermissionJpaEntity saved =
        jpaRepository.save(mapper.toJpa(rolePermission, role, permission));
    return mapper.toDomain(saved, rolePermission.getRole(), rolePermission.getPermission());
  }

  @Override
  public List<RolePermission> saveAll(List<RolePermission> rolePermissions) {
    List<RolePermissionJpaEntity> entities =
        rolePermissions.stream()
            .map(
                rp -> {
                  RoleJpaEntity role = roleJpaRepository.getReferenceById(rp.getRole().id());
                  PermissionJpaEntity permission =
                      permissionJpaRepository.getReferenceById(rp.getPermission().id());
                  return mapper.toJpa(rp, role, permission);
                })
            .toList();
    List<RolePermissionJpaEntity> saved = jpaRepository.saveAll(entities);
    List<RolePermission> result = new ArrayList<>(saved.size());
    for (int i = 0; i < saved.size(); i++) {
      result.add(
          mapper.toDomain(
              saved.get(i),
              rolePermissions.get(i).getRole(),
              rolePermissions.get(i).getPermission()));
    }
    return result;
  }

  @Override
  public Optional<RolePermission> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<RolePermission> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findByIdAndRole_Company_Id(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<RolePermission> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<RolePermission> findAllByRoleId(Long roleId) {
    return jpaRepository.findAllByRoleId(roleId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<RolePermission> findAllByRoleCompanyId(Long companyId) {
    return jpaRepository.findAllByRoleCompanyId(companyId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public void delete(Long id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public void deleteAllByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) return;
    jpaRepository.deleteAllByIdInBatch(ids);
  }

  @Override
  public int reactivate(Long id) {
    return jpaRepository.reactivate(id);
  }

  @Override
  public int reactivate(Long id, Long companyId) {
    return jpaRepository.reactivate(id, companyId);
  }

  @Override
  public int reactivateAllByIds(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return 0;
    return jpaRepository.reactivateAllByIds(ids);
  }

  @Override
  public Optional<Long> findDisabledIdByRoleAndPermission(Long roleId, Long permissionId) {
    return jpaRepository.findDisabledIdByRoleAndPermission(roleId, permissionId);
  }

  @Override
  public List<DisabledRolePermissionLookup> findDisabledByRoleAndPermissions(
      Long roleId, Collection<Long> permissionIds) {
    if (permissionIds == null || permissionIds.isEmpty()) return List.of();
    return jpaRepository.findDisabledByRoleAndPermissions(roleId, permissionIds).stream()
        .map(row -> new DisabledRolePermissionLookup(row.getId(), row.getPermissionId()))
        .toList();
  }
}
