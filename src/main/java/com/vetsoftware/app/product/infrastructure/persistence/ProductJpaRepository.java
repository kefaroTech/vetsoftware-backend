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

    @EntityGraph(attributePaths = {"productCategory", "tax", "company"})
    Optional<ProductJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE products SET enabled = true WHERE id = :id AND company_id = :companyId",
        nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
                   @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByTax_Id(Long taxId);

    boolean existsByProductCategory_Id(Long productCategoryId);

    boolean existsByIdAndCompany_Id(Long id, Long companyId);

    // @SQLRestriction("enabled = true") aplica: solo cuenta productos ACTIVOS (un code desactivado se reusa).
    boolean existsByCompany_IdAndCode(Long companyId, String code);

    boolean existsByCompany_IdAndCodeAndIdNot(Long companyId, String code, Long id);

    // @SQLRestriction("enabled = true") aplica: solo cuenta productos ACTIVOS (un name desactivado se reusa).
    boolean existsByCompany_IdAndName(Long companyId, String name);

    boolean existsByCompany_IdAndNameAndIdNot(Long companyId, String name, Long id);
}
