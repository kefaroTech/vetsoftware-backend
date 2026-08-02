package com.vetsoftware.app.rolepermission.infrastructure.cache;

import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RolePermissionCacheAdapter implements PermissionCachePort {

  private static final String CACHE_NAME = "employee-permissions";

  private final EmployeeRoleJpaRepository employeeRoleJpaRepository;
  private final CacheManager cacheManager;

  public RolePermissionCacheAdapter(
      EmployeeRoleJpaRepository employeeRoleJpaRepository, CacheManager cacheManager) {
    this.employeeRoleJpaRepository = employeeRoleJpaRepository;
    this.cacheManager = cacheManager;
  }

  @Override
  public void evictByRoleId(Long roleId) {
    if (roleId == null) return;
    List<Long> employeeIds = employeeRoleJpaRepository.findEmployeeIdsByRoleId(roleId);
    if (employeeIds.isEmpty()) return;
    runAfterCommit(() -> evictAll(employeeIds));
  }

  private void evictAll(List<Long> employeeIds) {
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache == null) return;
    for (Long id : employeeIds) cache.evict(id);
  }

  private void runAfterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
    } else {
      action.run();
    }
  }
}
