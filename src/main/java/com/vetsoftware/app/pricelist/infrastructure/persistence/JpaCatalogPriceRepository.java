package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.application.dto.LinkStateDto;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCatalogPriceRepository implements CatalogPriceRepository {

    private final CatalogPriceJpaRepository jpaRepository;
    private final CatalogPriceJpaMapper mapper;

    public JpaCatalogPriceRepository(CatalogPriceJpaRepository jpaRepository,
            CatalogPriceJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CatalogPrice save(CatalogPrice catalogPrice) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(catalogPrice)));
    }

    @Override
    public Optional<CatalogPrice> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * Por articulo, ciclo y tramo: es como se lee una tarifa y es tambien el orden
     * de las columnas de {@code uq_catalog_prices_tier}, asi que la ordenacion sale
     * del indice sin {@code filesort}. Desempate por {@code id} para que el orden
     * sea total entre paginas.
     */
    @Override
    public PageResult<CatalogPrice> findAllByPriceListId(Long priceListId, int page, int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "catalogItemId")
                .and(Sort.by(Sort.Direction.ASC, "billingCycle"))
                .and(Sort.by(Sort.Direction.ASC, "tierMin")).and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(jpaRepository.findAllByPriceListId(priceListId,
                Pages.request(page, pageSize, order)), mapper::toDomain);
    }

    @Override
    public List<CatalogPrice> findTierScope(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle) {
        return jpaRepository.findAllByPriceListIdAndCatalogItemIdAndBillingCycle(priceListId,
                catalogItemId, billingCycle).stream().map(mapper::toDomain).toList();
    }

    /**
     * Ordenado por {@code (articulo, ciclo, tramo)} para que el examen de cobertura
     * reciba cada grupo ya encadenado y su diagnostico sea reproducible: con dos
     * huecos, el que se reporta es siempre el mismo.
     */
    @Override
    public List<CatalogPrice> findAllTiers(Long priceListId) {
        return jpaRepository
                .findAllByPriceListIdOrderByCatalogItemIdAscBillingCycleAscTierMinAsc(priceListId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<LinkStateDto> findAnyByTier(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle, int tierMin) {
        String cycle = billingCycle.name();
        return jpaRepository.findAnyIdByTier(priceListId, catalogItemId, cycle, tierMin)
                .map(id -> new LinkStateDto(id, jpaRepository.countEnabledByTier(priceListId,
                        catalogItemId, cycle, tierMin) > 0));
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }

    @Override
    public long countByPriceListId(Long priceListId) {
        return jpaRepository.countByPriceListId(priceListId);
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
