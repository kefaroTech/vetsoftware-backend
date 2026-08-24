package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBundleComponentRepository implements BundleComponentRepository {

    private final BundleComponentJpaRepository jpaRepository;
    private final BundleComponentJpaMapper mapper;
    private final CatalogItemJpaRepository catalogItemJpaRepository;

    public JpaBundleComponentRepository(BundleComponentJpaRepository jpaRepository,
            BundleComponentJpaMapper mapper, CatalogItemJpaRepository catalogItemJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.catalogItemJpaRepository = catalogItemJpaRepository;
    }

    @Override
    public BundleComponent save(BundleComponent component) {
        CatalogItemJpaEntity bundleItem = catalogItemJpaRepository
                .getReferenceById(component.getBundleItemId());
        CatalogItemJpaEntity componentItem = catalogItemJpaRepository
                .getReferenceById(component.getComponentItemId());
        return mapper
                .toDomain(jpaRepository.save(mapper.toJpa(component, bundleItem, componentItem)));
    }

    @Override
    public Optional<BundleComponent> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<BundleComponent> findAllByBundleItemId(Long bundleItemId) {
        return jpaRepository.findAllByBundleItem_IdOrderByIdAsc(bundleItemId).stream()
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
    public Optional<LinkStateDto> findAnyByPair(Long bundleItemId, Long componentItemId) {
        return jpaRepository.findAnyIdByPair(bundleItemId, componentItemId)
                .map(id -> new LinkStateDto(id,
                        jpaRepository.countEnabledByPair(bundleItemId, componentItemId) > 0));
    }

    @Override
    public boolean existsActiveInvolving(Long catalogItemId) {
        return jpaRepository.existsByBundleItem_IdOrComponentItem_Id(catalogItemId, catalogItemId);
    }
}
