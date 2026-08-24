package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.DependencyEdge;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCatalogItemDependencyRepository implements CatalogItemDependencyRepository {

    private final CatalogItemDependencyJpaRepository jpaRepository;
    private final CatalogItemDependencyJpaMapper mapper;
    private final CatalogItemJpaRepository catalogItemJpaRepository;

    public JpaCatalogItemDependencyRepository(CatalogItemDependencyJpaRepository jpaRepository,
            CatalogItemDependencyJpaMapper mapper,
            CatalogItemJpaRepository catalogItemJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.catalogItemJpaRepository = catalogItemJpaRepository;
    }

    @Override
    public CatalogItemDependency save(CatalogItemDependency dependency) {
        CatalogItemJpaEntity catalogItem = catalogItemJpaRepository
                .getReferenceById(dependency.getCatalogItemId());
        CatalogItemJpaEntity relatedItem = catalogItemJpaRepository
                .getReferenceById(dependency.getRelatedItemId());
        return mapper
                .toDomain(jpaRepository.save(mapper.toJpa(dependency, catalogItem, relatedItem)));
    }

    @Override
    public Optional<CatalogItemDependency> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CatalogItemDependency> findAllByCatalogItemId(Long catalogItemId) {
        return jpaRepository.findAllByCatalogItem_IdOrderByIdAsc(catalogItemId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }

    @Override
    public Optional<LinkStateDto> findAnyByTriple(Long catalogItemId, Long relatedItemId,
            RelationType relationType) {
        String type = relationType.name();
        return jpaRepository.findAnyIdByTriple(catalogItemId, relatedItemId, type)
                .map(id -> new LinkStateDto(id, jpaRepository.countEnabledByTriple(catalogItemId,
                        relatedItemId, type) > 0));
    }

    @Override
    public List<DependencyEdge> findAllRequiresEdges() {
        return jpaRepository.findEdgesByRelationType(RelationType.REQUIRES).stream()
                .map(row -> new DependencyEdge((Long) row[0], (Long) row[1])).toList();
    }

    @Override
    public boolean existsActiveInvolving(Long catalogItemId) {
        return jpaRepository.existsByCatalogItem_IdOrRelatedItem_Id(catalogItemId, catalogItemId);
    }
}
