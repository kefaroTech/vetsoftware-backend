package com.vetsoftware.app.economicactivity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EconomicActivityJpaRepository extends JpaRepository<EconomicActivityJpaEntity, Long> {

    boolean existsByCode(String code);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE economic_activities SET enabled = true WHERE id = :id",
        nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
