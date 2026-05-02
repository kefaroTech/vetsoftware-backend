package com.vetsoftware.app.auth.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemUserPermissionJpaRepository
        extends JpaRepository<SystemUserPermissionJpaEntity, Long> {

    @Query("""
            SELECT sup.systemPermission.code
            FROM SystemUserPermissionJpaEntity sup
            WHERE sup.systemUserId = :systemUserId
            """)
    List<String> findPermissionCodesBySystemUserId(@Param("systemUserId") Long systemUserId);
}
