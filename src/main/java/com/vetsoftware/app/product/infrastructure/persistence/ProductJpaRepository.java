package com.vetsoftware.app.product.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long>,
        JpaSpecificationExecutor<ProductJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"productCategory", "tax", "company"})
    List<ProductJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"productCategory", "tax", "company"})
    Optional<ProductJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"productCategory", "tax", "company"})
    List<ProductJpaEntity> findAllByCompanyId(Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE products SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByTax_Id(Long taxId);

    boolean existsByProductCategory_Id(Long productCategoryId);

    boolean existsByIdAndCompany_Id(Long id, Long companyId);
}
