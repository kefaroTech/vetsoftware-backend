package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.ListPriceListsUseCase;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "pricelist.list")
@Service
public class ListPriceListsService implements ListPriceListsUseCase {

    private final PriceListRepository repository;

    public ListPriceListsService(PriceListRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PriceListDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(PriceListDto::from);
    }

    /**
     * Un {@code status} nulo cae en el listado completo en vez de devolver una
     * pagina vacia: el parametro es opcional en la ruta y ahi «no filtro» y «filtro
     * por nada» tienen que ser la misma cosa.
     */
    @Override
    public PageResult<PriceListDto> listByStatus(PriceListStatus status, int page, int pageSize) {
        if (status == null)
            return listAll(page, pageSize);
        return repository.findAllByStatus(status, page, pageSize).map(PriceListDto::from);
    }
}
