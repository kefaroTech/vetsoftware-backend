package com.vetsoftware.app.pricelist.application.port.out;

import com.vetsoftware.app.pricelist.application.dto.LinkStateDto;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

public interface PriceListRepository {

    PriceList save(PriceList priceList);

    Optional<PriceList> findById(Long id);

    /**
     * Misma carga que {@link #findById(Long)} pero tomando un
     * {@code PESSIMISTIC_WRITE} sobre la fila de la lista.
     *
     * <p>
     * Es lo que serializa el <em>read-then-write</em> de los precios que cuelgan de
     * ella: la comprobacion de solape de tramos lee sus hermanos y decide, y sin el
     * bloqueo dos altas concurrentes leen el mismo conjunto y las dos pasan.
     * {@code uq_catalog_prices_tier} atrapa solo el choque exacto de
     * {@code tier_min}; el solape parcial se cuela. Mismo patron que
     * {@code JpaEmployeeQueryPort.lockForOverlapCheck} en el flujo de citas.
     *
     * <p>
     * No va acotado por empresa porque esta tabla no tiene empresa: es tarifa
     * global de plataforma y sus puertos de entrada solo los alcanza SYSTEM.
     */
    Optional<PriceList> lockById(Long id);

    /**
     * El codigo <strong>ignorando el borrado logico</strong>, que es como lo mira
     * {@code uq_price_lists_code}. Es la guarda del alta: sin ella el choque lo
     * detecta la base y sale un 409 sin mensaje sobre una fila que nadie ve. Ver
     * {@link LinkStateDto}.
     */
    Optional<LinkStateDto> findAnyByCode(String code);

    PageResult<PriceList> findAll(int page, int pageSize);

    /**
     * Las tarifas de un estado, paginadas y con el mismo orden que
     * {@link #findAll(int, int)}. Filtrar en la base y no en el cliente es lo que
     * impide que una lista publicada se pierda por caer fuera de la primera pagina
     * (incidencia #450).
     */
    PageResult<PriceList> findAllByStatus(PriceListStatus status, int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
