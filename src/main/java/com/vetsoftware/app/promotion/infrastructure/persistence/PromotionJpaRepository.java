package com.vetsoftware.app.promotion.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionJpaRepository extends JpaRepository<PromotionJpaEntity, Long> {

  @Override
  @EntityGraph(attributePaths = "company")
  List<PromotionJpaEntity> findAll();

  @Override
  @EntityGraph(attributePaths = "company")
  Optional<PromotionJpaEntity> findById(Long id);

  @EntityGraph(attributePaths = "company")
  Optional<PromotionJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

  @EntityGraph(attributePaths = "company")
  List<PromotionJpaEntity> findAllByCompanyId(Long companyId);

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE promotions SET enabled = true WHERE id = :id",
      nativeQuery = true)
  int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
