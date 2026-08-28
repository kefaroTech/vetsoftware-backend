package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.entitlement.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.entitlement.domain.LimitDimensionRef;
import com.vetsoftware.app.entitlement.domain.MeasureKind;
import com.vetsoftware.app.limitdimension.infrastructure.persistence.LimitDimensionJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resuelve el eje del catalogo por su codigo. Junto con
 * {@code JpaCompanyCapacityRepository}, uno de los dos unicos archivos de esta
 * feature que conocen {@code limitdimension}, por la excepcion acotada del
 * {@code CLAUDE.md}.
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
@Component("entitlementJpaLimitDimensionQueryPort")
public class JpaLimitDimensionQueryPort implements LimitDimensionQueryPort {

    private final LimitDimensionJpaRepository limitDimensionJpaRepository;

    public JpaLimitDimensionQueryPort(LimitDimensionJpaRepository limitDimensionJpaRepository) {
        this.limitDimensionJpaRepository = limitDimensionJpaRepository;
    }

    /**
     * <strong>Trae tambien {@code available_from}</strong>, que es la mitad que
     * faltaba de D-74: la columna existia desde el changeset 300 y no la leia
     * nadie, asi que la regla que el contador aplicaba seguia siendo la vieja --sin
     * fila, techo cero, jamas ilimitado-- incluso para un eje que nacio despues de
     * que el cliente firmara.
     */
    @Override
    public Optional<LimitDimensionRef> findByCode(String code) {
        return limitDimensionJpaRepository.findByCode(code)
                .map(entity -> new LimitDimensionRef(entity.getId(), entity.getCode(),
                        MeasureKind.valueOf(entity.getMeasureKind()), entity.getAvailableFrom()));
    }
}
