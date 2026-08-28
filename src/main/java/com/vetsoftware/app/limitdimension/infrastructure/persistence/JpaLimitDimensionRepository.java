package com.vetsoftware.app.limitdimension.infrastructure.persistence;

import com.vetsoftware.app.limitdimension.application.port.out.LimitDimensionRepository;
import com.vetsoftware.app.limitdimension.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.limitdimension.domain.LimitDimension;
import com.vetsoftware.app.limitdimension.domain.SubModuleRef;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de salida del catálogo de ejes.
 *
 * <p>
 * Hidrata el {@link SubModuleRef} con {@link SubModuleQueryPort} en lugar de
 * con un grafo de entidad sobre una asociación, porque la entidad no cuelga
 * ninguna: ver el javadoc de {@link LimitDimensionJpaEntity} para el porqué. Un
 * submódulo apuntado que ya no se puede resolver deja el {@code subModule} en
 * {@code null} en vez de reventar la lectura; la clave foránea es
 * {@code RESTRICT}, así que ese estado no debería existir, y si existe conviene
 * poder abrir la pantalla para arreglarlo.
 */
@Repository
public class JpaLimitDimensionRepository implements LimitDimensionRepository {

    private final LimitDimensionJpaRepository jpaRepository;
    private final LimitDimensionJpaMapper mapper;
    private final SubModuleQueryPort subModuleQueryPort;

    public JpaLimitDimensionRepository(LimitDimensionJpaRepository jpaRepository,
            LimitDimensionJpaMapper mapper, SubModuleQueryPort subModuleQueryPort) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.subModuleQueryPort = subModuleQueryPort;
    }

    @Override
    public LimitDimension save(LimitDimension dimension) {
        return toDomain(jpaRepository.save(mapper.toJpa(dimension)));
    }

    @Override
    public Optional<LimitDimension> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<LimitDimension> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public List<LimitDimension> findAllOrderedByCode() {
        return jpaRepository.findAllByOrderByCodeAsc().stream().map(this::toDomain).toList();
    }

    private LimitDimension toDomain(LimitDimensionJpaEntity entity) {
        SubModuleRef ref = entity.getSubModuleId() == null
                ? null
                : subModuleQueryPort.findById(entity.getSubModuleId()).orElse(null);
        return mapper.toDomain(entity, ref);
    }
}
