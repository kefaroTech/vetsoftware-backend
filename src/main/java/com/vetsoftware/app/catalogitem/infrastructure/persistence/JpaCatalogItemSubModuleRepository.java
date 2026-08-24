package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemSubModuleRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCatalogItemSubModuleRepository implements CatalogItemSubModuleRepository {

    private final CatalogItemSubModuleJpaRepository jpaRepository;
    private final CatalogItemSubModuleJpaMapper mapper;
    private final CatalogItemJpaRepository catalogItemJpaRepository;
    private final SubModuleJpaRepository subModuleJpaRepository;

    public JpaCatalogItemSubModuleRepository(CatalogItemSubModuleJpaRepository jpaRepository,
            CatalogItemSubModuleJpaMapper mapper, CatalogItemJpaRepository catalogItemJpaRepository,
            SubModuleJpaRepository subModuleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.catalogItemJpaRepository = catalogItemJpaRepository;
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    /**
     * {@code getReferenceById} devuelve un proxy sin SELECT: la existencia de las
     * dos filas ya la validó el caso de uso, y volver a leerlas aquí serían dos
     * consultas por alta que no deciden nada.
     */
    @Override
    public CatalogItemSubModule save(CatalogItemSubModule link) {
        CatalogItemJpaEntity catalogItem = catalogItemJpaRepository
                .getReferenceById(link.getCatalogItemId());
        SubModuleJpaEntity subModule = subModuleJpaRepository
                .getReferenceById(link.getSubModule().id());
        CatalogItemSubModuleJpaEntity saved = jpaRepository
                .save(mapper.toJpa(link, catalogItem, subModule));
        return mapper.toDomain(saved, link.getSubModule());
    }

    @Override
    public Optional<CatalogItemSubModule> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CatalogItemSubModule> findAllByCatalogItemId(Long catalogItemId) {
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
    public Optional<LinkStateDto> findAnyByPair(Long catalogItemId, Long subModuleId) {
        return jpaRepository.findAnyIdByPair(catalogItemId, subModuleId)
                .map(id -> new LinkStateDto(id,
                        jpaRepository.countEnabledByPair(catalogItemId, subModuleId) > 0));
    }

    @Override
    public boolean existsActiveByCatalogItemId(Long catalogItemId) {
        return jpaRepository.existsByCatalogItem_Id(catalogItemId);
    }
}
