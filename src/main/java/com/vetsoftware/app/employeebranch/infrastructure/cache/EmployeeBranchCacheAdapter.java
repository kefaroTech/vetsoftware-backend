package com.vetsoftware.app.employeebranch.infrastructure.cache;

import com.vetsoftware.app.employeebranch.application.port.out.BranchAccessCachePort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Invalida el cache Redis {@code employee-branch-ids} (poblado por {@code JpaBranchAccessResolver}) tras reasignar
 * las sedes de un empleado. Mismo patrón que {@code EmployeeRoleCacheAdapter}: se evicta DESPUÉS del commit para no
 * borrar el cache si la transacción termina haciendo rollback.
 */
@Component
public class EmployeeBranchCacheAdapter implements BranchAccessCachePort {

    private static final String CACHE_NAME = "employee-branch-ids";

    private final CacheManager cacheManager;

    public EmployeeBranchCacheAdapter(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void evictByEmployeeId(Long employeeId) {
        if (employeeId == null) return;
        runAfterCommit(() -> {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) cache.evict(employeeId);
        });
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
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
