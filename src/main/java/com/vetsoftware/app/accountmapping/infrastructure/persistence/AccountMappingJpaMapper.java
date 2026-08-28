package com.vetsoftware.app.accountmapping.infrastructure.persistence;

import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos</strong>, y de eso depende que
 * el cierre de una vigencia sea una edicion y no un insert: con la version en
 * {@code null} sobre una entidad que ya tiene id, Hibernate la tomaria por
 * transitoria y el {@code merge} escribiria una fila nueva — dos mapeos para el
 * mismo supuesto, que es justo lo que las columnas generadas del changeset 343
 * existen para impedir.
 *
 * <p>
 * <strong>No toca ninguna de las cuatro columnas generadas</strong>: las
 * calcula MySQL y no estan mapeadas. Escribirlas desde aqui haria que el motor
 * rechazara el {@code INSERT}.
 */
@Component
public class AccountMappingJpaMapper {

    public AccountMappingJpaEntity toJpa(AccountMapping mapping) {
        AccountMappingJpaEntity entity = new AccountMappingJpaEntity();
        entity.setId(mapping.getId());
        entity.setMappingKind(mapping.getMappingKind());
        entity.setMappingKey(mapping.getMappingKey());
        entity.setCatalogItemId(mapping.getCatalogItemId());
        entity.setChargeType(mapping.getChargeType());
        entity.setTaxTreatment(mapping.getTaxTreatment());
        entity.setDebitAccountCode(mapping.getDebitAccountCode());
        entity.setCreditAccountCode(mapping.getCreditAccountCode());
        entity.setDeferredAccountCode(mapping.getDeferredAccountCode());
        entity.setValidFrom(mapping.getValidFrom());
        entity.setValidTo(mapping.getValidTo());
        entity.setCreatedDate(mapping.getCreatedDate());
        entity.setEnabled(mapping.isEnabled());
        entity.setVersion(mapping.getVersion());
        return entity;
    }

    public AccountMapping toDomain(AccountMappingJpaEntity entity) {
        return new AccountMapping(entity.getId(), entity.getMappingKind(), entity.getMappingKey(),
                entity.getCatalogItemId(), entity.getChargeType(), entity.getTaxTreatment(),
                entity.getDebitAccountCode(), entity.getCreditAccountCode(),
                entity.getDeferredAccountCode(), entity.getValidFrom(), entity.getValidTo(),
                entity.getCreatedDate(), entity.isEnabled(), entity.getVersion());
    }
}
