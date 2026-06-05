package com.vetsoftware.app.openaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OpenAccountJpaRepository extends JpaRepository<OpenAccountJpaEntity, Long>,
        JpaSpecificationExecutor<OpenAccountJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"owner", "company", "createdBy"})
    List<OpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"owner", "company", "createdBy"})
    Optional<OpenAccountJpaEntity> findById(Long id);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE open_accounts SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
