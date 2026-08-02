package com.vetsoftware.app.consultationtype.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationTypeJpaRepository
        extends
            JpaRepository<ConsultationTypeJpaEntity, Long> {

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE consultation_types SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
