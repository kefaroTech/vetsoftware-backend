package com.vetsoftware.app.auth.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true WHERE r.id = :id")
    int revokeById(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true "
            + "WHERE r.subjectId = :subjectId AND r.subjectType = :subjectType AND r.revoked = false")
    int revokeAllForSubject(@Param("subjectId") Long subjectId, @Param("subjectType") String subjectType);
}
