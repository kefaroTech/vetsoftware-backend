package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.goodsreceipt.domain.BranchRef;
import com.vetsoftware.app.goodsreceipt.domain.CompanyRef;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.ProductRef;
import com.vetsoftware.app.goodsreceipt.domain.SupplierRef;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GoodsReceiptJpaMapper {

    /**
     * Write path: arma la entidad reusando las referencias JPA (proxies via
     * getReferenceById) que le pasa el repositorio; {@code productsById} mapea cada
     * productId a su proxy.
     */
    public GoodsReceiptJpaEntity toJpa(GoodsReceipt receipt, CompanyJpaEntity company,
            BranchJpaEntity branch, SupplierJpaEntity supplier,
            Map<Long, ProductJpaEntity> productsById) {
        GoodsReceiptJpaEntity entity = new GoodsReceiptJpaEntity();
        entity.setId(receipt.getId());
        entity.setCompany(company);
        entity.setBranch(branch);
        entity.setSupplier(supplier);
        entity.setPurchaseOrderId(receipt.getPurchaseOrderId());
        entity.setReceiptDate(receipt.getReceiptDate());
        entity.setSupplierInvoiceNumber(receipt.getSupplierInvoiceNumber());
        entity.setNotes(receipt.getNotes());
        entity.setStatus(receipt.getStatus());
        entity.setCreatedDate(receipt.getCreatedDate());
        entity.setCreatedBy(receipt.getCreatedBy());
        entity.setUpdatedDate(receipt.getUpdatedDate());
        entity.setUpdatedBy(receipt.getUpdatedBy());
        entity.setVersion(receipt.getVersion());
        entity.setEnabled(receipt.isEnabled());
        for (GoodsReceiptLine line : receipt.getLines()) {
            GoodsReceiptLineJpaEntity lineEntity = new GoodsReceiptLineJpaEntity();
            lineEntity.setId(line.getId());
            lineEntity.setProduct(productsById.get(line.getProduct().id()));
            lineEntity.setPurchaseOrderLineId(line.getPurchaseOrderLineId());
            lineEntity.setLotNumber(line.getLotNumber());
            lineEntity.setExpireDate(line.getExpireDate());
            lineEntity.setQuantityReceived(line.getQuantityReceived());
            lineEntity.setUnitCost(line.getUnitCost());
            entity.addLine(lineEntity);
        }
        return entity;
    }

    /**
     * Read path: el @EntityGraph ya hidrató
     * company/branch/supplier/lines/lines.product.
     */
    public GoodsReceipt toDomain(GoodsReceiptJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        BranchJpaEntity b = entity.getBranch();
        SupplierJpaEntity s = entity.getSupplier();
        List<GoodsReceiptLine> lines = entity.getLines().stream().map(le -> {
            ProductJpaEntity p = le.getProduct();
            return new GoodsReceiptLine(le.getId(),
                    new ProductRef(p.getId(), p.getName(), p.getCode()),
                    le.getPurchaseOrderLineId(), le.getLotNumber(), le.getExpireDate(),
                    le.getQuantityReceived(), le.getUnitCost());
        }).toList();
        return new GoodsReceipt(entity.getId(),
                new CompanyRef(c.getId(), c.getName(), c.getIdentifier()),
                new BranchRef(b.getId(), b.getName()), new SupplierRef(s.getId(), s.getName()),
                entity.getPurchaseOrderId(), entity.getReceiptDate(),
                entity.getSupplierInvoiceNumber(), entity.getNotes(), entity.getStatus(), lines,
                entity.getCreatedDate(), entity.getCreatedBy(), entity.getUpdatedDate(),
                entity.getUpdatedBy(), entity.getVersion(), entity.isEnabled());
    }

    /**
     * Write-return path: rebuild del dominio reusando los Ref precargados del
     * agregado original (evita hidratar los proxies getReferenceById), tomando del
     * entity persistido los ids generados (recepción + líneas, en orden).
     */
    public GoodsReceipt toDomainReusingRefs(GoodsReceiptJpaEntity saved, GoodsReceipt original) {
        List<GoodsReceiptLineJpaEntity> savedLines = saved.getLines();
        List<GoodsReceiptLine> originalLines = original.getLines();
        List<GoodsReceiptLine> lines = new java.util.ArrayList<>(savedLines.size());
        for (int i = 0; i < savedLines.size(); i++) {
            GoodsReceiptLine ol = originalLines.get(i);
            lines.add(new GoodsReceiptLine(savedLines.get(i).getId(), ol.getProduct(),
                    ol.getPurchaseOrderLineId(), ol.getLotNumber(), ol.getExpireDate(),
                    ol.getQuantityReceived(), ol.getUnitCost()));
        }
        return new GoodsReceipt(saved.getId(), original.getCompany(), original.getBranch(),
                original.getSupplier(), original.getPurchaseOrderId(), original.getReceiptDate(),
                original.getSupplierInvoiceNumber(), original.getNotes(), original.getStatus(),
                lines, saved.getCreatedDate(), original.getCreatedBy(), saved.getUpdatedDate(),
                original.getUpdatedBy(), saved.getVersion(), saved.isEnabled());
    }
}
