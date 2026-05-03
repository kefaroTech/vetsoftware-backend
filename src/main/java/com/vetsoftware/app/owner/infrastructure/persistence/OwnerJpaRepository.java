package com.vetsoftware.app.owner.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OwnerJpaRepository extends JpaRepository<OwnerJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"city", "company"})
    List<OwnerJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"city", "company"})
    Optional<OwnerJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"city", "company"})
    @Query("SELECT o FROM OwnerJpaEntity o WHERE o.company.id = :companyId AND ("
        + "LOWER(o.name) LIKE LOWER(CONCAT('%', :query, '%')) OR "
        + "LOWER(o.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<OwnerJpaEntity> searchByCompanyAndNameOrEmail(@Param("companyId") Long companyId,
                                                       @Param("query") String query);
}
