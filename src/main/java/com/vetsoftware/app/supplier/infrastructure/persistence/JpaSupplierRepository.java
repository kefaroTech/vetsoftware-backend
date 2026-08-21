package com.vetsoftware.app.supplier.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.supplier.application.command.SearchSuppliersCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.Supplier;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSupplierRepository implements SupplierRepository {
    private final SupplierJpaRepository jpaRepository;
    private final SupplierJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaSupplierRepository(SupplierJpaRepository jpaRepository, SupplierJpaMapper mapper,
            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Supplier save(Supplier supplier) {
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(supplier.getCompany().id());
        SupplierJpaEntity saved = jpaRepository.saveAndFlush(mapper.toJpa(supplier, company));
        return mapper.toDomain(saved, supplier.getCompany());
    }

    @Override
    public Optional<Supplier> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Supplier> findByIdAndCompanyId(Long id, Long companyId) {
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
    public List<Supplier> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Supplier> search(SearchSuppliersCommand command) {
        Specification<SupplierJpaEntity> spec = buildSpec(command);
        Page<SupplierJpaEntity> page = jpaRepository.findAll(spec, Pages.request(command.page(),
                command.pageSize(), Sort.by(Sort.Direction.DESC, "createdDate")));
        return Pages.result(page, mapper::toDomain);
    }

    private Specification<SupplierJpaEntity> buildSpec(SearchSuppliersCommand command) {
        return (root, query, cb) -> {
            // fetch-join del @ManyToOne solo en la query de datos (no en la de count) para
            // evitar N+1
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("company", JoinType.LEFT);
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("id"), command.companyId()));
            if (command.name() != null && !command.name().isBlank()) {
                predicates.add(cb.like(root.get("name"), "%" + command.name() + "%"));
            }
            if (command.taxId() != null && !command.taxId().isBlank()) {
                predicates.add(cb.like(root.get("taxId"), "%" + command.taxId() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
