package com.vetsoftware.app.purchaseorder.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.purchaseorder.domain.BranchRef;
import com.vetsoftware.app.purchaseorder.domain.CompanyRef;
import com.vetsoftware.app.purchaseorder.domain.ProductRef;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.domain.SupplierRef;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderJpaMapper {

    public PurchaseOrderJpaEntity toJpa(PurchaseOrder order, CompanyJpaEntity company, BranchJpaEntity branch,
                                        SupplierJpaEntity supplier, LongFunction<ProductJpaEntity> productRef) {
        PurchaseOrderJpaEntity entity = new PurchaseOrderJpaEntity();
        entity.setId(order.getId());
        entity.setCompany(company);
        entity.setBranch(branch);
        entity.setSupplier(supplier);
        entity.setStatus(order.getStatus());
        entity.setOrderDate(order.getOrderDate());
        entity.setExpectedDate(order.getExpectedDate());
        entity.setNotes(order.getNotes());
        entity.setCreatedDate(order.getCreatedDate());
        entity.setCreatedBy(order.getCreatedBy());
        entity.setUpdatedDate(order.getUpdatedDate());
        entity.setUpdatedBy(order.getUpdatedBy());
        entity.setVersion(order.getVersion());
        entity.setEnabled(order.isEnabled());

        List<PurchaseOrderLineJpaEntity> lines = new ArrayList<>();
        for (PurchaseOrderLine line : order.getLines()) {
            PurchaseOrderLineJpaEntity le = new PurchaseOrderLineJpaEntity();
            le.setId(line.getId());
            le.setProduct(productRef.apply(line.getProduct().id()));
            le.setQuantityOrdered(line.getQuantityOrdered());
            le.setUnitCost(line.getUnitCost());
            le.setQuantityReceived(line.getQuantityReceived());
            lines.add(le);
        }
        entity.setLines(lines);
        return entity;
    }

    // Read path — el @EntityGraph ya hidrató company/branch/supplier y las líneas con su producto.
    public PurchaseOrder toDomain(PurchaseOrderJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        BranchJpaEntity b = entity.getBranch();
        SupplierJpaEntity s = entity.getSupplier();
        List<PurchaseOrderLine> lines = entity.getLines().stream().map(le -> {
            ProductJpaEntity p = le.getProduct();
            return new PurchaseOrderLine(le.getId(), new ProductRef(p.getId(), p.getName(), p.getCode()),
                le.getQuantityOrdered(), le.getUnitCost(), le.getQuantityReceived());
        }).toList();
        return toDomain(entity,
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()),
            new BranchRef(b.getId(), b.getName()),
            new SupplierRef(s.getId(), s.getName()),
            lines);
    }

    // Write path — reusa los refs precargados; los productos de las líneas se resuelven por id sin
    // inicializar el proxy de getReferenceById (el id está disponible en el proxy sin SELECT).
    public PurchaseOrder toDomain(PurchaseOrderJpaEntity entity, CompanyRef companyRef, BranchRef branchRef,
                                  SupplierRef supplierRef, Map<Long, ProductRef> productRefs) {
        List<PurchaseOrderLine> lines = entity.getLines().stream().map(le ->
            new PurchaseOrderLine(le.getId(), productRefs.get(le.getProduct().getId()),
                le.getQuantityOrdered(), le.getUnitCost(), le.getQuantityReceived())).toList();
        return toDomain(entity, companyRef, branchRef, supplierRef, lines);
    }

    private PurchaseOrder toDomain(PurchaseOrderJpaEntity entity, CompanyRef companyRef, BranchRef branchRef,
                                   SupplierRef supplierRef, List<PurchaseOrderLine> lines) {
        return new PurchaseOrder(
            entity.getId(),
            companyRef,
            branchRef,
            supplierRef,
            entity.getStatus(),
            entity.getOrderDate(),
            entity.getExpectedDate(),
            entity.getNotes(),
            lines,
            entity.getCreatedDate(),
            entity.getCreatedBy(),
            entity.getUpdatedDate(),
            entity.getUpdatedBy(),
            entity.getVersion(),
            entity.isEnabled());
    }
}
