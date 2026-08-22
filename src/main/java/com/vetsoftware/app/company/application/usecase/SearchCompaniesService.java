package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.SearchCompaniesUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "company.search")
@Service
public class SearchCompaniesService implements SearchCompaniesUseCase {
    private final CompanyRepository repository;

    public SearchCompaniesService(CompanyRepository repository) {
        this.repository = repository;
    }

    /**
     * El {@code companyId} viaja igual que en el listado y decide el alcance antes
     * que el término: filtrar no puede sacar a la luz una fila que el actor no
     * podría listar. Un término vacío devuelve lo mismo que el listado, que es lo
     * que espera un buscador cuando se borra lo escrito.
     */
    @Override
    public PageResult<CompanyDto> search(Long companyId, String query, int page, int pageSize) {
        return repository.searchVisibleTo(companyId, query, page, pageSize).map(CompanyDto::from);
    }
}
