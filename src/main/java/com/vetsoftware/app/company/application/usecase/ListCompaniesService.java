package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "company.list")
@Service
public class ListCompaniesService implements ListCompaniesUseCase {
    private final CompanyRepository repository;

    public ListCompaniesService(CompanyRepository repository) {
        this.repository = repository;
    }

    /**
     * {@code companyId == null} solo lo produce un principal de plataforma
     * —{@code Authz.currentCompanyIdOrNull()} devuelve la empresa del empleado y
     * {@code null} para SYSTEM—, y es el único caso en que se lee el registro
     * completo. Un empleado llega siempre con su empresa, y entonces «listar
     * empresas» es exactamente una fila: la suya. Se devuelve como página, y no
     * como recurso único, porque el alcance no cambia la forma del contrato.
     *
     * <p>
     * Una empresa que no existe (o que se borró entre la autenticación y esta
     * lectura) devuelve página vacía y no un 404: es un listado, y un listado sin
     * resultados no es un error.
     *
     * <p>
     * Los totales son los de la consulta y salen del adaptador; aquí solo se mapea
     * el contenido con {@link PageResult#map}, que los conserva intactos.
     */
    @Override
    public PageResult<CompanyDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAllVisibleTo(companyId, page, pageSize).map(CompanyDto::from);
    }
}
