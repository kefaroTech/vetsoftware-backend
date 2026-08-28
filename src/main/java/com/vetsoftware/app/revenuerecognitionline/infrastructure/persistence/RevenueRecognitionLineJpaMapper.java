package com.vetsoftware.app.revenuerecognitionline.infrastructure.persistence;

import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>No copia ninguna version, porque no la hay</strong>: la entidad esta
 * exenta ({@code E1_APPEND_ONLY}). Eso significa tambien que {@code toJpa} solo
 * se usa para insertar: un {@code merge} sobre una fila existente reescribiria
 * un renglon del libro, que es justo lo que este modelo prohibe.
 */
@Component
public class RevenueRecognitionLineJpaMapper {

    public RevenueRecognitionLineJpaEntity toJpa(RevenueRecognitionLine line) {
        RevenueRecognitionLineJpaEntity entity = new RevenueRecognitionLineJpaEntity();
        entity.setId(line.getId());
        entity.setCompanyId(line.getCompanyId());
        entity.setChargeId(line.getChargeId());
        entity.setPeriodKey(line.getPeriodKey());
        entity.setPostingPeriod(line.getPostingPeriod());
        entity.setRecognizedAmount(line.getRecognizedAmount());
        entity.setMethod(line.getMethod());
        entity.setCreatedDate(line.getCreatedDate());
        return entity;
    }

    public RevenueRecognitionLine toDomain(RevenueRecognitionLineJpaEntity entity) {
        return new RevenueRecognitionLine(entity.getId(), entity.getCompanyId(),
                entity.getChargeId(), entity.getPeriodKey(), entity.getPostingPeriod(),
                entity.getRecognizedAmount(), entity.getMethod(), entity.getCreatedDate());
    }
}
