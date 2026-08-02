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
  @Query("SELECT o FROM OwnerJpaEntity o WHERE o.id = :id AND o.company.id = :companyId")
  Optional<OwnerJpaEntity> findByIdAndCompanyId(
      @Param("id") Long id, @Param("companyId") Long companyId);

  @EntityGraph(attributePaths = {"city", "company"})
  @Query("SELECT o FROM OwnerJpaEntity o WHERE o.company.id = :companyId")
  List<OwnerJpaEntity> findAllByCompanyId(@Param("companyId") Long companyId);

  @EntityGraph(attributePaths = {"city", "company"})
  @Query(
      "SELECT o FROM OwnerJpaEntity o WHERE o.company.id = :companyId AND ("
          + "LOWER(o.name) LIKE LOWER(CONCAT('%', :query, '%')) OR "
          + "LOWER(o.email) LIKE LOWER(CONCAT('%', :query, '%')) OR "
          + "LOWER(o.document) LIKE LOWER(CONCAT('%', :query, '%')))")
  List<OwnerJpaEntity> searchByCompanyAndTerm(
      @Param("companyId") Long companyId, @Param("query") String query);

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE owners SET enabled = true WHERE id = :id AND company_id = :companyId",
      nativeQuery = true)
  int reactivate(
      @org.springframework.data.repository.query.Param("id") Long id,
      @org.springframework.data.repository.query.Param("companyId") Long companyId);

  boolean existsByCity_Id(Long cityId);

  boolean existsByCompany_Id(Long companyId);
}
