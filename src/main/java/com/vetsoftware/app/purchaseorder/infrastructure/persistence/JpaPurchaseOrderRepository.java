package com.vetsoftware.app.purchaseorder.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.purchaseorder.application.command.SearchPurchaseOrdersCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PageResult;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.ProductRef;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPurchaseOrderRepository implements PurchaseOrderRepository {
    private final PurchaseOrderJpaRepository jpaRepository;
    private final PurchaseOrderJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;
    private final BranchJpaRepository branchJpaRepository;
    private final SupplierJpaRepository supplierJpaRepository;
    private final ProductJpaRepository productJpaRepository;

    public JpaPurchaseOrderRepository(PurchaseOrderJpaRepository jpaRepository,
            PurchaseOrderJpaMapper mapper, CompanyJpaRepository companyJpaRepository,
            BranchJpaRepository branchJpaRepository, SupplierJpaRepository supplierJpaRepository,
            ProductJpaRepository productJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
        this.branchJpaRepository = branchJpaRepository;
        this.supplierJpaRepository = supplierJpaRepository;
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public PurchaseOrder save(PurchaseOrder order) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(order.getCompany().id());
        BranchJpaEntity branch = branchJpaRepository.getReferenceById(order.getBranch().id());
        SupplierJpaEntity supplier = supplierJpaRepository
                .getReferenceById(order.getSupplier().id());
        PurchaseOrderJpaEntity toSave = mapper.toJpa(order, company, branch, supplier,
                productJpaRepository::getReferenceById);
        PurchaseOrderJpaEntity saved = jpaRepository.saveAndFlush(toSave);

        Map<Long, ProductRef> productRefs = order.getLines().stream()
                .map(PurchaseOrderLine::getProduct)
                .collect(Collectors.toMap(ProductRef::id, Function.identity(), (a, b) -> a));
        return mapper.toDomain(saved, order.getCompany(), order.getBranch(), order.getSupplier(),
                productRefs);
    }

    @Override
    public Optional<PurchaseOrder> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PurchaseOrder> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<PurchaseOrder> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompany_IdOrderByOrderDateDescCreatedDateDesc(companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PurchaseOrder> findAllDisabledByCompanyId(Long companyId) {
        return jpaRepository.findAllDisabledByCompany_Id(companyId).stream().map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<PurchaseOrder> search(SearchPurchaseOrdersCommand command) {
        Specification<PurchaseOrderJpaEntity> spec = buildSpec(command);
        PageRequest pageRequest = PageRequest.of(Math.max(command.page(), 0),
                command.pageSize() <= 0 ? 20 : command.pageSize(),
                Sort.by(Sort.Direction.DESC, "orderDate")
                        .and(Sort.by(Sort.Direction.DESC, "createdDate")));
        Page<PurchaseOrderJpaEntity> page = jpaRepository.findAll(spec, pageRequest);
        List<PurchaseOrder> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    private Specification<PurchaseOrderJpaEntity> buildSpec(SearchPurchaseOrdersCommand command) {
        return (root, query, cb) -> {
            // fetch-join de los @ManyToOne solo en la query de datos (no en la de count)
            // para evitar N+1.
            // Las líneas (colección) NO se fetch-join aquí: se hidratan LAZY dentro de la
            // tx de lectura
            // para no
            // paginar en memoria.
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
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), command.from()));
            }
            if (command.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), command.to()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
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
