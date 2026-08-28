package com.vetsoftware.app.supplierwithholding.infrastructure.persistence;

import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos</strong>, y de eso depende que
 * emitir el certificado sea una edicion y no un insert: con la version en
 * {@code null} sobre una entidad que ya tiene id, Hibernate la tomaria por
 * transitoria y escribiria una fila nueva que chocaria contra
 * {@code uq_supplier_withholdings_case}.
 *
 * <p>
 * <strong>Es tambien el unico sitio donde el año cambia de forma</strong>
 * ({@code int} en el dominio, {@code short} en la entidad, porque la columna es
 * {@code SMALLINT}). El {@code cast} es seguro: el constructor del dominio ya
 * rechazo cualquier año fuera de 2020..2100.
 *
 * <p>
 * <strong>No toca {@code municipality_key}</strong>: la calcula MySQL y no esta
 * mapeada.
 */
@Component
public class SupplierWithholdingJpaMapper {

    public SupplierWithholdingJpaEntity toJpa(SupplierWithholding withholding) {
        SupplierWithholdingJpaEntity entity = new SupplierWithholdingJpaEntity();
        entity.setId(withholding.getId());
        entity.setSupplierTaxId(withholding.getSupplierTaxId());
        entity.setSupplierName(withholding.getSupplierName());
        entity.setSupplierDocType(withholding.getSupplierDocType());
        entity.setSupplierInvoiceRef(withholding.getSupplierInvoiceRef());
        entity.setWithholdingType(withholding.getWithholdingType());
        entity.setConcept(withholding.getConcept());
        entity.setTaxableBase(withholding.getTaxableBase());
        entity.setRatePercent(withholding.getRatePercent());
        entity.setAmount(withholding.getAmount());
        entity.setMunicipalityCode(withholding.getMunicipalityCode());
        entity.setFiscalYear((short) withholding.getFiscalYear());
        entity.setFiscalPeriodKey(withholding.getFiscalPeriodKey());
        entity.setPracticedOn(withholding.getPracticedOn());
        entity.setCertificateIssuedAt(withholding.getCertificateIssuedAt());
        entity.setCertificateRef(withholding.getCertificateRef());
        entity.setPaymentReceiptRef(withholding.getPaymentReceiptRef());
        entity.setCreatedDate(withholding.getCreatedDate());
        entity.setVersion(withholding.getVersion());
        return entity;
    }

    public SupplierWithholding toDomain(SupplierWithholdingJpaEntity entity) {
        return new SupplierWithholding(entity.getId(), entity.getSupplierTaxId(),
                entity.getSupplierName(), entity.getSupplierDocType(),
                entity.getSupplierInvoiceRef(), entity.getWithholdingType(), entity.getConcept(),
                entity.getTaxableBase(), entity.getRatePercent(), entity.getAmount(),
                entity.getMunicipalityCode(), entity.getFiscalYear(), entity.getFiscalPeriodKey(),
                entity.getPracticedOn(), entity.getCertificateIssuedAt(),
                entity.getCertificateRef(), entity.getPaymentReceiptRef(), entity.getCreatedDate(),
                entity.getVersion());
    }
}
