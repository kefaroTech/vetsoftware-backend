package com.vetsoftware.app.withholdingraterule.infrastructure.persistence;

import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos, y de eso depende que el cierre
 * de una vigencia sea una edicion y no un insert.</strong> Si {@code toJpa}
 * dejara la version en {@code null} sobre una entidad que ya tiene id,
 * Hibernate la tomaria por transitoria y el {@code merge} escribiria una fila
 * nueva —dos vigencias para el mismo supuesto, que es justo lo que las columnas
 * generadas del changeset 317 existen para impedir—.
 *
 * <p>
 * <strong>No toca {@code municipality_key} ni
 * {@code current_rule_marker}</strong>: las calcula MySQL y no estan mapeadas.
 * Escribirlas desde aqui haria que el motor rechazara el {@code INSERT}.
 */
@Component
public class WithholdingRateRuleJpaMapper {

    public WithholdingRateRuleJpaEntity toJpa(WithholdingRateRule rule) {
        WithholdingRateRuleJpaEntity entity = new WithholdingRateRuleJpaEntity();
        entity.setId(rule.getId());
        entity.setWithholdingType(rule.getWithholdingType());
        entity.setServiceNature(rule.getServiceNature());
        entity.setMunicipalityCode(rule.getMunicipalityCode());
        entity.setRatePercent(rule.getRatePercent());
        entity.setMinimumBaseAmount(rule.getMinimumBaseAmount());
        entity.setMinimumBaseUvt(rule.getMinimumBaseUvt());
        entity.setLegalReference(rule.getLegalReference());
        entity.setValidFrom(rule.getValidFrom());
        entity.setValidTo(rule.getValidTo());
        entity.setCreatedDate(rule.getCreatedDate());
        entity.setEnabled(rule.isEnabled());
        entity.setVersion(rule.getVersion());
        return entity;
    }

    public WithholdingRateRule toDomain(WithholdingRateRuleJpaEntity entity) {
        return new WithholdingRateRule(entity.getId(), entity.getWithholdingType(),
                entity.getServiceNature(), entity.getMunicipalityCode(), entity.getRatePercent(),
                entity.getMinimumBaseAmount(), entity.getMinimumBaseUvt(),
                entity.getLegalReference(), entity.getValidFrom(), entity.getValidTo(),
                entity.getCreatedDate(), entity.isEnabled(), entity.getVersion());
    }
}
