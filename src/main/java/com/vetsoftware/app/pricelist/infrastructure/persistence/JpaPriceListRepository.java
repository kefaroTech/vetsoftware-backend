package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.application.dto.LinkStateDto;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPriceListRepository implements PriceListRepository {

    private final PriceListJpaRepository jpaRepository;
    private final PriceListJpaMapper mapper;

    public JpaPriceListRepository(PriceListJpaRepository jpaRepository, PriceListJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PriceList save(PriceList priceList) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(priceList)));
    }

    @Override
    public Optional<PriceList> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PriceList> lockById(Long id) {
        return jpaRepository.lockById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LinkStateDto> findAnyByCode(String code) {
        return jpaRepository.findAnyIdByCode(code)
                .map(id -> new LinkStateDto(id, jpaRepository.countEnabledByCode(code) > 0));
    }

    /**
     * Las mas recientes primero, que es como se busca una tarifa. El orden es total
     * -desempate por {@code id}-: sin el, dos paginas consecutivas repiten u omiten
     * filas cuando varias listas comparten {@code valid_from}, que es justo lo que
     * pasa el dia que se publican la mensual y la anual del mismo trimestre.
     */
    @Override
    public PageResult<PriceList> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, recentFirst())),
                mapper::toDomain);
    }

    private static Sort recentFirst() {
        return Sort.by(Sort.Direction.DESC, "validFrom").and(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * Mismo orden que {@link #findAll(int, int)}: el filtro no cambia como se lee
     * una tarifa.
     */
    @Override
    public PageResult<PriceList> findAllByStatus(PriceListStatus status, int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAllByStatus(status, Pages.request(page, pageSize, recentFirst())),
                mapper::toDomain);
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
