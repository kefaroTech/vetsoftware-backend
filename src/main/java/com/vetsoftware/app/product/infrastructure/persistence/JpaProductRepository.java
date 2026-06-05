package com.vetsoftware.app.product.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaEntity;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaRepository;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProductRepository implements ProductRepository {
    private final ProductJpaRepository jpaRepository;
    private final ProductJpaMapper mapper;
    private final ProductCategoryJpaRepository productCategoryJpaRepository;
    private final TaxJpaRepository taxJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaProductRepository(ProductJpaRepository jpaRepository,
                                ProductJpaMapper mapper,
                                ProductCategoryJpaRepository productCategoryJpaRepository,
                                TaxJpaRepository taxJpaRepository,
                                CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.productCategoryJpaRepository = productCategoryJpaRepository;
        this.taxJpaRepository = taxJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Product save(Product product) {
        ProductCategoryJpaEntity productCategory =
            productCategoryJpaRepository.getReferenceById(product.getProductCategory().id());
        TaxJpaEntity tax = product.getTax() == null ? null
            : taxJpaRepository.getReferenceById(product.getTax().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(product.getCompany().id());
        ProductJpaEntity saved = jpaRepository.save(mapper.toJpa(product, productCategory, tax, company));
        return mapper.toDomain(saved, product.getProductCategory(), product.getTax(), product.getCompany());
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Product> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Product> search(SearchProductsCommand command) {
        Specification<ProductJpaEntity> spec = buildSpec(command);
        PageRequest pageRequest = PageRequest.of(
            Math.max(command.page(), 0),
            command.pageSize() <= 0 ? 20 : command.pageSize(),
            Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<ProductJpaEntity> page = jpaRepository.findAll(spec, pageRequest);
        List<Product> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }

    private Specification<ProductJpaEntity> buildSpec(SearchProductsCommand command) {
        return (root, query, cb) -> {
            // fetch-join de los @ManyToOne solo en la query de datos (no en la de count) para evitar N+1
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("productCategory", JoinType.LEFT);
                root.fetch("tax", JoinType.LEFT);
                root.fetch("company", JoinType.LEFT);
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("id"), command.companyId()));
            if (command.name() != null && !command.name().isBlank()) {
                predicates.add(cb.like(root.get("name"), "%" + command.name() + "%"));
            }
            if (command.code() != null && !command.code().isBlank()) {
                predicates.add(cb.like(root.get("code"), "%" + command.code() + "%"));
            }
            if (command.productCategoryId() != null) {
                predicates.add(cb.equal(root.get("productCategory").get("id"), command.productCategoryId()));
            }
            if (command.taxId() != null) {
                predicates.add(cb.equal(root.get("tax").get("id"), command.taxId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
