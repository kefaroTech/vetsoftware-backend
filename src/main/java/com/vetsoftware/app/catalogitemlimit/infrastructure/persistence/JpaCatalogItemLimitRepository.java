package com.vetsoftware.app.catalogitemlimit.infrastructure.persistence;

import com.vetsoftware.app.catalogitemlimit.application.port.out.CatalogItemLimitRepository;
import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador de salida de los techos de fábrica. */
@Repository
public class JpaCatalogItemLimitRepository implements CatalogItemLimitRepository {

    private final CatalogItemLimitJpaRepository jpaRepository;
    private final CatalogItemLimitJpaMapper mapper;

    public JpaCatalogItemLimitRepository(CatalogItemLimitJpaRepository jpaRepository,
            CatalogItemLimitJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CatalogItemLimit save(CatalogItemLimit limit) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(limit)));
    }

    @Override
    public Optional<CatalogItemLimit> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CatalogItemLimit> findByIdAndCatalogItemId(Long id, Long catalogItemId) {
        return jpaRepository.findByIdAndCatalogItemId(id, catalogItemId).map(mapper::toDomain);
    }

    @Override
    public Optional<CatalogItemLimit> findByCatalogItemIdAndLimitDimensionId(Long catalogItemId,
            Long limitDimensionId) {
        return jpaRepository.findByCatalogItemIdAndLimitDimensionId(catalogItemId, limitDimensionId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByCatalogItemIdAndLimitDimensionId(Long catalogItemId,
            Long limitDimensionId) {
        return jpaRepository.existsByCatalogItemIdAndLimitDimensionId(catalogItemId,
                limitDimensionId);
    }

    @Override
    public List<CatalogItemLimit> findAllByCatalogItemId(Long catalogItemId) {
        return jpaRepository.findAllByCatalogItemIdOrderByLimitDimensionIdAsc(catalogItemId)
                .stream().map(mapper::toDomain).toList();
    }
}
