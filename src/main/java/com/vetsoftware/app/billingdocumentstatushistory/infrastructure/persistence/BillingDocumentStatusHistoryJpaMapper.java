package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.persistence;

import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Un solo {@code toDomain} y sin sobrecarga de camino de
 * escritura</strong>: el dominio no guarda ningun companion VO, solo los ids de
 * las FK, asi que no hay proxy que se pueda disparar al reconstruir el
 * fotograma.
 *
 * <p>
 * <strong>Y sin {@code version} que arrastrar en ningun sentido</strong>,
 * porque la tabla no la tiene. La trampa clasica del mapper de una entidad
 * versionada —perder la version y que Hibernate inserte una fila duplicada en
 * vez de actualizar— aqui no puede darse: la unica operacion es el
 * {@code INSERT}, y el {@code id} nulo de una entidad recien registrada es
 * exactamente lo que Hibernate necesita para verlo asi.
 *
 * <p>
 * <strong>{@code toDomain} vuelve a pasar por el constructor del dominio y por
 * tanto por sus validaciones.</strong> Es a proposito: si una fila antigua
 * violara {@code chk_bdsh_transition} —porque alguien la escribiera por SQL
 * crudo saltandose el agregado— la lectura fallaria en vez de devolver un
 * fotograma imposible que despues cuadraria mal el informe.
 */
@Component
public class BillingDocumentStatusHistoryJpaMapper {

    public BillingDocumentStatusHistoryJpaEntity toJpa(BillingDocumentStatusHistory entry) {
        BillingDocumentStatusHistoryJpaEntity entity = new BillingDocumentStatusHistoryJpaEntity();
        entity.setId(entry.getId());
        entity.setCompanyId(entry.getCompanyId());
        entity.setBillingDocumentId(entry.getBillingDocumentId());
        entity.setFromStatus(entry.getFromStatus());
        entity.setToStatus(entry.getToStatus());
        entity.setOccurredAt(entry.getOccurredAt());
        entity.setActor(entry.getActor());
        entity.setReason(entry.getReason());
        entity.setCreatedDate(entry.getCreatedDate());
        return entity;
    }

    public BillingDocumentStatusHistory toDomain(BillingDocumentStatusHistoryJpaEntity entity) {
        return new BillingDocumentStatusHistory(entity.getId(), entity.getCompanyId(),
                entity.getBillingDocumentId(), entity.getFromStatus(), entity.getToStatus(),
                entity.getOccurredAt(), entity.getActor(), entity.getReason(),
                entity.getCreatedDate());
    }
}
