package com.vetsoftware.app.limitdimension.infrastructure.persistence;

import com.vetsoftware.app.limitdimension.domain.LimitDimension;
import com.vetsoftware.app.limitdimension.domain.MeasureKind;
import com.vetsoftware.app.limitdimension.domain.SubModuleRef;
import org.springframework.stereotype.Component;

/** El único sitio que conoce a la vez el eje de dominio y su fila. */
@Component
public class LimitDimensionJpaMapper {

    public LimitDimensionJpaEntity toJpa(LimitDimension dimension) {
        LimitDimensionJpaEntity entity = new LimitDimensionJpaEntity();
        entity.setId(dimension.getId());
        entity.setCode(dimension.getCode());
        entity.setName(dimension.getName());
        entity.setMeasureKind(dimension.getMeasureKind().name());
        entity.setSubModuleId(
                dimension.getSubModule() == null ? null : dimension.getSubModule().id());
        entity.setReleaseDelayDays(dimension.getReleaseDelayDays());
        entity.setAvailableFrom(dimension.getAvailableFrom());
        entity.setCreatedDate(dimension.getCreatedDate());
        entity.setEnabled(dimension.isEnabled());
        entity.setVersion(dimension.getVersion());
        return entity;
    }

    public LimitDimension toDomain(LimitDimensionJpaEntity entity, SubModuleRef subModule) {
        return new LimitDimension(entity.getId(), entity.getCode(), entity.getName(),
                MeasureKind.valueOf(entity.getMeasureKind()), subModule,
                entity.getReleaseDelayDays(), entity.getAvailableFrom(), entity.getCreatedDate(),
                entity.isEnabled(), entity.getVersion());
    }
}
