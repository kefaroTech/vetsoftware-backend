package com.vetsoftware.app.supplierinvoice.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaRepository;
import com.vetsoftware.app.supplierinvoice.application.command.SearchSupplierInvoicesCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.PageResult;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
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
public class JpaSupplierInvoiceRepository implements SupplierInvoiceRepository {
    private final SupplierInvoiceJpaRepository jpaRepository;
    private final SupplierInvoiceJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;
    private final BranchJpaRepository branchJpaRepository;
    private final SupplierJpaRepository supplierJpaRepository;

    public JpaSupplierInvoiceRepository(SupplierInvoiceJpaRepository jpaRepository,
                                        SupplierInvoiceJpaMapper mapper,
                                        CompanyJpaRepository companyJpaRepository,
                                        BranchJpaRepository branchJpaRepository,
                                        SupplierJpaRepository supplierJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
        this.branchJpaRepository = branchJpaRepository;
        this.supplierJpaRepository = supplierJpaRepository;
    }

    @Override
    public SupplierInvoice save(SupplierInvoice invoice) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(invoice.getCompany().id());
        BranchJpaEntity branch = branchJpaRepository.getReferenceById(invoice.getBranch().id());
        SupplierJpaEntity supplier = supplierJpaRepository.getReferenceById(invoice.getSupplier().id());
        SupplierInvoiceJpaEntity saved = jpaRepository.saveAndFlush(
            mapper.toJpa(invoice, company, branch, supplier));
        return mapper.toDomainReusingRefs(saved, invoice);
    }

    @Override
    public Optional<SupplierInvoice> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<SupplierInvoice> search(SearchSupplierInvoicesCommand command) {
        Specification<SupplierInvoiceJpaEntity> spec = buildSpec(command);
        PageRequest pageRequest = PageRequest.of(
            Math.max(command.page(), 0),
            command.pageSize() <= 0 ? 20 : command.pageSize(),
            Sort.by(Sort.Direction.DESC, "issueDate").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<SupplierInvoiceJpaEntity> page = jpaRepository.findAll(spec, pageRequest);
        List<SupplierInvoice> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }

    private Specification<SupplierInvoiceJpaEntity> buildSpec(SearchSupplierInvoicesCommand command) {
        return (root, query, cb) -> {
            // fetch-join solo de los @ManyToOne (no la colección de abonos, para no romper la paginación) y solo en
            // la query de datos (no en la de count). Los abonos se cargan LAZY al mapear dentro de la tx readOnly.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("company", JoinType.LEFT);
                root.fetch("branch", JoinType.LEFT);
                root.fetch("supplier", JoinType.LEFT);
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("id"), command.companyId()));
            if (command.supplierId() != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), command.supplierId()));
            }
            if (command.branchId() != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), command.branchId()));
            }
            if (command.status() != null) {
                predicates.add(cb.equal(root.get("status"), command.status()));
            }
            if (command.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issueDate"), command.from()));
            }
            if (command.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issueDate"), command.to()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<SupplierInvoice> findOutstandingByCompany(Long companyId, Long branchId) {
        List<SupplierInvoiceStatus> outstanding =
            List.of(SupplierInvoiceStatus.PENDING, SupplierInvoiceStatus.PARTIAL);
        List<SupplierInvoiceJpaEntity> entities = branchId == null
            ? jpaRepository.findAllByCompany_IdAndStatusInOrderByDueDateAsc(companyId, outstanding)
            : jpaRepository.findAllByCompany_IdAndBranch_IdAndStatusInOrderByDueDateAsc(companyId, branchId, outstanding);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByCompanySupplierAndNumber(Long companyId, Long supplierId, String invoiceNumber) {
        return jpaRepository.existsByCompany_IdAndSupplier_IdAndInvoiceNumber(companyId, supplierId, invoiceNumber);
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
