package com.vetsoftware.app.productcategory.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProductCategoryRepository implements ProductCategoryRepository {
    private final ProductCategoryJpaRepository jpaRepository;
    private final ProductCategoryJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaProductCategoryRepository(ProductCategoryJpaRepository jpaRepository,
            ProductCategoryJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public ProductCategory save(ProductCategory productCategory) {
        CompanyJpaEntity company = productCategory.getCompany() == null
                ? null
                : companyJpaRepository.getReferenceById(productCategory.getCompany().id());
        ProductCategoryJpaEntity saved = jpaRepository
                .saveAndFlush(mapper.toJpa(productCategory, company));
        return mapper.toDomain(saved, productCategory.getCompany());
    }

    @Override
    public Optional<ProductCategory> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProductCategory> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCompanyIdAndName(Long companyId, String name) {
        return jpaRepository.existsByCompany_IdAndName(companyId, name);
    }

    @Override
    public boolean existsByCompanyIdAndNameExcludingId(Long companyId, String name, Long id) {
        return jpaRepository.existsByCompany_IdAndNameAndIdNot(companyId, name, id);
    }

    @Override
    public List<ProductCategory> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProductCategory> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompany_Id(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
