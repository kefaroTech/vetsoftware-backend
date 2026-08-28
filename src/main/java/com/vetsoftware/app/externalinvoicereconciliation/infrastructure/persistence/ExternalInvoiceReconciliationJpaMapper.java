package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Un solo {@code toDomain} y sin sobrecarga de camino de
 * escritura</strong>: el dominio no guarda ningun companion VO, solo los ids de
 * las FK, asi que no hay proxy que se pueda disparar al reconstruir la
 * conciliacion.
 *
 * <p>
 * <strong>La {@code version} viaja en los dos sentidos</strong>, y sin ella el
 * bloqueo optimista no serviria de nada: el adaptador guarda con
 * {@code save(...)} sobre una entidad JPA <em>nueva</em> construida aqui, asi
 * que si {@code toJpa} no arrastrara la version leida, Hibernate haria un
 * {@code merge} con version nula y sobreescribiria el cambio del otro operador
 * en silencio — exactamente lo que {@code @Version} existe para impedir.
 */
@Component
public class ExternalInvoiceReconciliationJpaMapper {

    public ExternalInvoiceReconciliationJpaEntity toJpa(
            ExternalInvoiceReconciliation reconciliation) {
        ExternalInvoiceReconciliationJpaEntity entity = new ExternalInvoiceReconciliationJpaEntity();
        entity.setId(reconciliation.getId());
        entity.setCompanyId(reconciliation.getCompanyId());
        entity.setBillingDocumentId(reconciliation.getBillingDocumentId());
        entity.setExternalResolutionNumber(reconciliation.getExternalResolutionNumber());
        entity.setExternalRangeFrom(reconciliation.getExternalRangeFrom());
        entity.setExternalRangeTo(reconciliation.getExternalRangeTo());
        entity.setResolutionValidUntil(reconciliation.getResolutionValidUntil());
        entity.setExternalInvoiceId(reconciliation.getExternalInvoiceId());
        entity.setExternalCufe(reconciliation.getExternalCufe());
        entity.setComputedTotal(reconciliation.getComputedTotal());
        entity.setComputedTax(reconciliation.getComputedTax());
        entity.setExternalTotal(reconciliation.getExternalTotal());
        entity.setExternalTax(reconciliation.getExternalTax());
        entity.setDifference(reconciliation.getDifference());
        entity.setStatus(reconciliation.getStatus());
        entity.setResolvedBySystemUserId(reconciliation.getResolvedBySystemUserId());
        entity.setResolvedAt(reconciliation.getResolvedAt());
        entity.setResolutionNote(reconciliation.getResolutionNote());
        entity.setPostingPeriod(reconciliation.getPostingPeriod());
        entity.setCreatedDate(reconciliation.getCreatedDate());
        entity.setVersion(reconciliation.getVersion());
        return entity;
    }

    public ExternalInvoiceReconciliation toDomain(ExternalInvoiceReconciliationJpaEntity entity) {
        return new ExternalInvoiceReconciliation(entity.getId(), entity.getCompanyId(),
                entity.getBillingDocumentId(), entity.getExternalResolutionNumber(),
                entity.getExternalRangeFrom(), entity.getExternalRangeTo(),
                entity.getResolutionValidUntil(), entity.getExternalInvoiceId(),
                entity.getExternalCufe(), entity.getComputedTotal(), entity.getComputedTax(),
                entity.getExternalTotal(), entity.getExternalTax(), entity.getDifference(),
                entity.getStatus(), entity.getResolvedBySystemUserId(), entity.getResolvedAt(),
                entity.getResolutionNote(), entity.getPostingPeriod(), entity.getCreatedDate(),
                entity.getVersion());
    }
}
