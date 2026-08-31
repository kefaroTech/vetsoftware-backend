package com.vetsoftware.app.catalogitemaihint.infrastructure.persistence;

import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import org.springframework.stereotype.Component;

/**
 * {@code hintHash} no se copia en ninguna direccion: la escribe la base
 * ({@code GENERATED ALWAYS}) y el dominio no la guarda, porque la huella no es
 * un dato de la pista sino la forma en que el indice la identifica.
 */
@Component
public class CatalogItemAiHintJpaMapper {

    public CatalogItemAiHintJpaEntity toJpa(CatalogItemAiHint hint) {
        CatalogItemAiHintJpaEntity entity = new CatalogItemAiHintJpaEntity();
        entity.setId(hint.getId());
        entity.setCatalogItemId(hint.getCatalogItemId());
        entity.setHintRevision(hint.getHintRevision());
        entity.setHintText(hint.getHintText());
        entity.setPublishedAt(hint.getPublishedAt());
        entity.setPublishedBySystemUserId(hint.getPublishedBySystemUserId());
        entity.setSupersededAt(hint.getSupersededAt());
        entity.setSupersededBySystemUserId(hint.getSupersededBySystemUserId());
        entity.setCreatedDate(hint.getCreatedDate());
        entity.setVersion(hint.getVersion());
        return entity;
    }

    public CatalogItemAiHint toDomain(CatalogItemAiHintJpaEntity entity) {
        return new CatalogItemAiHint(entity.getId(), entity.getCatalogItemId(),
                entity.getHintRevision(), entity.getHintText(), entity.getPublishedAt(),
                entity.getPublishedBySystemUserId(), entity.getSupersededAt(),
                entity.getSupersededBySystemUserId(), entity.getCreatedDate(), entity.getVersion());
    }
}
