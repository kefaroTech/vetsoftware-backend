package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.ListDisabledCompaniesUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.list.disabled")
@Service
public class ListDisabledCompaniesService implements ListDisabledCompaniesUseCase {
    private final CompanyRepository repository;

    public ListDisabledCompaniesService(CompanyRepository repository) {
        this.repository = repository;
    }

    /**
     * Mismo reparto de ramas que {@link ListCompaniesService#listAll}:
     * {@code companyId == null} solo lo produce un principal de plataforma y es el
     * unico caso en que se lee el archivo completo; un empleado llega siempre con
     * su empresa y entonces «listar archivadas» es como mucho una fila, la suya.
     *
     * <p>
     * <b>{@code @Transactional(readOnly = true)} no es decorativo.</b> La consulta
     * que hay debajo es SQL nativo —es la unica forma de esquivar el
     * {@code @SQLRestriction}— y una consulta nativa no admite
     * {@code @EntityGraph}, asi que {@code city} llega como proxy perezoso y se
     * hidrata al mapear. Con {@code spring.jpa.open-in-view: false}
     * (application.yml) no hay sesion abierta en la capa web: sin esta transaccion
     * el mapeo revienta con {@code LazyInitializationException} en la primera fila.
     * Mismo criterio que {@code ListEmployeesByCompanyService}, que pagina la otra
     * tabla con {@code @SQLRestriction} del sistema.
     *
     * <p>
     * El precio es un {@code SELECT} de ciudad por fila —N+1 acotado por
     * {@code Pages.MAX_SIZE}, 200 como maximo—, y se acepta a sabiendas: el archivo
     * de empresas es por naturaleza un conjunto pequeño y se consulta desde una
     * pantalla de administracion, no desde un camino caliente. Lo que no se acepta
     * es perder el filtro: ver {@code CompanyRepository#findAllDisabledVisibleTo}.
     *
     * <p>
     * Los totales son los de la consulta y salen del adaptador; aqui solo se mapea
     * el contenido con {@link PageResult#map}, que los conserva intactos. Un
     * archivo vacio es pagina vacia, nunca un 404: un listado sin resultados no es
     * un error.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<CompanyDto> listDisabled(Long companyId, int page, int pageSize) {
        return repository.findAllDisabledVisibleTo(companyId, page, pageSize).map(CompanyDto::from);
    }
}
