package com.vetsoftware.app.employeerole.infrastructure.cache;

import com.vetsoftware.app.employeerole.application.port.out.PermissionCachePort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EmployeeRoleCacheAdapter implements PermissionCachePort {

    private static final String CACHE_NAME = "employee-permissions";

    private final CacheManager cacheManager;

    public EmployeeRoleCacheAdapter(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void evictByEmployeeId(Long employeeId) {
        if (employeeId == null)
            return;
        runAfterCommit(() -> {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null)
                cache.evict(employeeId);
        });
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager
                    .registerSynchronization(new TransactionSynchronization() {
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
