package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
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
     * empresas» es exactamente una fila: la suya. Se devuelve como lista, y no como
     * recurso único, para no cambiar la forma del JSON que ya consumen los dos
     * fronts.
     *
     * <p>
     * Una empresa que no existe (o que se borró entre la autenticación y esta
     * lectura) devuelve lista vacía y no un 404: es un listado, y un listado sin
     * resultados no es un error.
     */
    @Override
    public List<CompanyDto> listAll(Long companyId) {
        return repository.findAllVisibleTo(companyId).stream().map(CompanyDto::from).toList();
    }
}
