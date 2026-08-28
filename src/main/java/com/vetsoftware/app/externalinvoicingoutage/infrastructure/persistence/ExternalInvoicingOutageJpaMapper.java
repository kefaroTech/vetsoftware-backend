package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos, y de eso depende que cerrar una
 * caida sea una edicion y no un insert.</strong> Si {@code toJpa} dejara la
 * version en {@code null} sobre una entidad que ya tiene id, Hibernate la
 * tomaria por transitoria y el {@code merge} escribiria una fila nueva —dos
 * caidas para el mismo tramo, una viva y otra cerrada, que es justo lo que
 * {@code uq_eio_open} existe para impedir—.
 *
 * <p>
 * <strong>No toca {@code open_outage_marker}</strong>: lo calcula MySQL y no
 * esta mapeado. Escribirlo desde aqui haria que el motor rechazara el
 * {@code INSERT}.
 */
@Component
public class ExternalInvoicingOutageJpaMapper {

    public ExternalInvoicingOutageJpaEntity toJpa(ExternalInvoicingOutage outage) {
        ExternalInvoicingOutageJpaEntity entity = new ExternalInvoicingOutageJpaEntity();
        entity.setId(outage.getId());
        entity.setStartedAt(outage.getStartedAt());
        entity.setEndedAt(outage.getEndedAt());
        entity.setCauseParty(outage.getCauseParty());
        entity.setSummary(outage.getSummary());
        entity.setAffectedCompanyCount(outage.getAffectedCompanyCount());
        entity.setNotifiedCompaniesAt(outage.getNotifiedCompaniesAt());
        entity.setExternalIncidentRef(outage.getExternalIncidentRef());
        entity.setCreatedDate(outage.getCreatedDate());
        entity.setVersion(outage.getVersion());
        return entity;
    }

    public ExternalInvoicingOutage toDomain(ExternalInvoicingOutageJpaEntity entity) {
        return new ExternalInvoicingOutage(entity.getId(), entity.getStartedAt(),
                entity.getEndedAt(), entity.getCauseParty(), entity.getSummary(),
                entity.getAffectedCompanyCount(), entity.getNotifiedCompaniesAt(),
                entity.getExternalIncidentRef(), entity.getCreatedDate(), entity.getVersion());
    }
}
