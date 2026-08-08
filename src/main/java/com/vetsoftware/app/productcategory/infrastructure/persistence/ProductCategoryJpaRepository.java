package com.vetsoftware.app.productcategory.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryJpaRepository
        extends
            JpaRepository<ProductCategoryJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<ProductCategoryJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<ProductCategoryJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<ProductCategoryJpaEntity> findAllByCompany_Id(Long companyId);

    @EntityGraph(attributePaths = "company")
    Optional<ProductCategoryJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE product_categories
            SET enabled = true
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByIdAndCompany_Id(Long id, Long companyId);

    // @SQLRestriction("enabled = true") aplica: solo cuenta categorías ACTIVAS (un
    // name desactivado
    // se reusa).
    boolean existsByCompany_IdAndName(Long companyId, String name);

    boolean existsByCompany_IdAndNameAndIdNot(Long companyId, String name, Long id);
}
