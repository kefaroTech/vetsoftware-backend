package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.goodsreceipt.application.command.SearchGoodsReceiptsCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.PageResult;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class JpaGoodsReceiptRepository implements GoodsReceiptRepository {
    private final GoodsReceiptJpaRepository jpaRepository;
    private final GoodsReceiptJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;
    private final BranchJpaRepository branchJpaRepository;
    private final SupplierJpaRepository supplierJpaRepository;
    private final ProductJpaRepository productJpaRepository;

    public JpaGoodsReceiptRepository(GoodsReceiptJpaRepository jpaRepository,
            GoodsReceiptJpaMapper mapper, CompanyJpaRepository companyJpaRepository,
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
    public GoodsReceipt save(GoodsReceipt receipt) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(receipt.getCompany().id());
        BranchJpaEntity branch = branchJpaRepository.getReferenceById(receipt.getBranch().id());
        SupplierJpaEntity supplier = supplierJpaRepository
                .getReferenceById(receipt.getSupplier().id());
        Map<Long, ProductJpaEntity> productsById = new HashMap<>();
        for (GoodsReceiptLine line : receipt.getLines()) {
            productsById.computeIfAbsent(line.getProduct().id(),
                    productJpaRepository::getReferenceById);
        }
        GoodsReceiptJpaEntity saved = jpaRepository
                .saveAndFlush(mapper.toJpa(receipt, company, branch, supplier, productsById));
        return mapper.toDomainReusingRefs(saved, receipt);
    }

    @Override
    public Optional<GoodsReceipt> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<GoodsReceipt> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompany_IdOrderByReceiptDateDescIdDesc(companyId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<GoodsReceipt> search(SearchGoodsReceiptsCommand command) {
        Specification<GoodsReceiptJpaEntity> spec = buildSpec(command);
        PageRequest pageRequest = PageRequest.of(Math.max(command.page(), 0),
                command.pageSize() <= 0 ? 20 : command.pageSize(),
                Sort.by(Sort.Direction.DESC, "receiptDate")
                        .and(Sort.by(Sort.Direction.DESC, "id")));
        Page<GoodsReceiptJpaEntity> page = jpaRepository.findAll(spec, pageRequest);
        List<GoodsReceipt> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    private Specification<GoodsReceiptJpaEntity> buildSpec(SearchGoodsReceiptsCommand command) {
        return (root, query, cb) -> {
            // fetch-join solo de los @ManyToOne (no la colección de líneas, para no romper
            // la paginación)
            // y solo en
            // la query de datos (no en la de count). Las líneas se cargan LAZY al mapear
            // dentro de la tx
            // readOnly.
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
                predicates.add(cb.greaterThanOrEqualTo(root.get("receiptDate"), command.from()));
            }
            if (command.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receiptDate"), command.to()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
