package com.vetsoftware.app.catalogitemlimit.infrastructure.persistence;

import com.vetsoftware.app.catalogitemlimit.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.catalogitemlimit.domain.LimitDimensionRef;
import com.vetsoftware.app.catalogitemlimit.domain.MeasureKind;
import com.vetsoftware.app.limitdimension.infrastructure.persistence.LimitDimensionJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El único archivo de esta feature que conoce {@code limitdimension}, por la
 * excepción acotada del {@code CLAUDE.md}.
 *
 * <p>
 * <strong>El nombre de bean va explicito, y conviene decir por que NO.</strong>
 * No es porque el contexto se rompa: {@code VetSoftwareApplication} declara
 * {@code nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class}, asi
 * que el nombre de cada bean es su clase cualificada y repetir el nombre simple
 * es inocuo — hay veinte grupos de clases homonimas conviviendo, cinco
 * {@code JpaAnimalChildrenQueryPort} entre ellos. Va explicito por dos razones
 * mas modestas: las otras cuatro rodajas que declaran esta misma clase ya lo
 * hacen, y un contexto de test que escanee por nombre simple en vez de importar
 * —lo que {@code PersistenceSliceConfig} evita a proposito con {@code @Import}—
 * si moriria con {@code ConflictingBeanDefinitionException}.
 */
@Component("catalogItemLimitJpaLimitDimensionQueryPort")
public class JpaLimitDimensionQueryPort implements LimitDimensionQueryPort {

    private final LimitDimensionJpaRepository limitDimensionJpaRepository;

    public JpaLimitDimensionQueryPort(LimitDimensionJpaRepository limitDimensionJpaRepository) {
        this.limitDimensionJpaRepository = limitDimensionJpaRepository;
    }

    @Override
    public Optional<LimitDimensionRef> findById(Long limitDimensionId) {
        return limitDimensionJpaRepository.findById(limitDimensionId)
                .map(entity -> new LimitDimensionRef(entity.getId(), entity.getCode(),
                        MeasureKind.valueOf(entity.getMeasureKind())));
    }
}
