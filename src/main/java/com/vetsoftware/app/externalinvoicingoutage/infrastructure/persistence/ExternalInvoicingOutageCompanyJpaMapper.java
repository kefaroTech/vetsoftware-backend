package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import org.springframework.stereotype.Component;

/**
 * Traduce el reparto entre dominio y JPA.
 *
 * <p>
 * <strong>{@code toJpa} recibe la caida ya resuelta</strong> —el
 * {@code getReferenceById} lo hace el adaptador— para no disparar el proxy: la
 * puente guarda el identificador de la caida y nada mas, asi que inicializar la
 * asociacion seria un {@code SELECT} para leer un numero que ya se tenia.
 *
 * <p>
 * <strong>{@code toDomain} lee {@code outage.getId()} y no navega mas
 * adentro.</strong> Con el {@code @EntityGraph} de las lecturas la asociacion
 * ya viene hidratada, y aun sin el, pedir solo el identificador de un proxy de
 * Hibernate no lo inicializa. No hay {@code N+1} escondido en esta linea.
 *
 * <p>
 * Sin version que copiar: la tabla no la tiene ({@code E2_TABLA_PUENTE}).
 */
@Component
public class ExternalInvoicingOutageCompanyJpaMapper {

    public ExternalInvoicingOutageCompanyJpaEntity toJpa(ExternalInvoicingOutageCompany affected,
            ExternalInvoicingOutageJpaEntity outage) {
        ExternalInvoicingOutageCompanyJpaEntity entity = new ExternalInvoicingOutageCompanyJpaEntity();
        entity.setId(affected.getId());
        entity.setOutage(outage);
        entity.setCompanyId(affected.getCompanyId());
        entity.setFailedDocumentCount(affected.getFailedDocumentCount());
        entity.setResolvedBy(affected.getResolvedBy());
        return entity;
    }

    public ExternalInvoicingOutageCompany toDomain(ExternalInvoicingOutageCompanyJpaEntity entity) {
        return new ExternalInvoicingOutageCompany(entity.getId(), entity.getOutage().getId(),
                entity.getCompanyId(), entity.getFailedDocumentCount(), entity.getResolvedBy());
    }
}
