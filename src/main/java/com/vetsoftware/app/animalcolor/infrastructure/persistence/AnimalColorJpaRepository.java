package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalColorJpaRepository extends JpaRepository<AnimalColorJpaEntity, Long> {

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE animal_colors SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
