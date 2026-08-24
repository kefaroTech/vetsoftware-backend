package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanyRepository implements CompanyRepository {

    /**
     * Orden de los dos listados: por nombre, que es como se lee un registro de
     * empresas, con el id de desempate. Sin un orden total la paginación no es
     * determinista y una misma fila puede salir en dos páginas —o en ninguna—.
     */
    private static final Sort BY_NAME_THEN_ID = Sort.by(Sort.Direction.ASC, "name")
            .and(Sort.by(Sort.Direction.ASC, "id"));

    private final CompanyJpaRepository jpaRepository;
    private final CompanyJpaMapper mapper;
    private final CityJpaRepository cityJpaRepository;

    public JpaCompanyRepository(CompanyJpaRepository jpaRepository, CompanyJpaMapper mapper,
            CityJpaRepository cityJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.cityJpaRepository = cityJpaRepository;
    }

    @Override
    public Company save(Company company) {
        CityJpaEntity city = cityJpaRepository.getReferenceById(company.getCity().id());
        CompanyJpaEntity saved = jpaRepository.save(mapper.toJpa(company, city));
        return mapper.toDomain(saved, company.getCity());
    }

    @Override
    public Optional<Company> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * La rama sin acotar vive aqui y no en el caso de uso a proposito: asi el
     * puerto no ofrece ninguna forma de pedir el registro completo sin declarar el
     * alcance, y un futuro caso de uso no puede tropezar con un {@code findAll()}
     * disponible. Con la empresa informada, «listar empresas» es exactamente una
     * fila —la suya—, que es la unica que el empleado tiene derecho a ver.
     *
     * <p>
     * Las dos ramas devuelven un {@code Page}, asi que los metadatos salen siempre
     * de la consulta y no de aritmetica escrita a mano. El tamaño lo acota
     * {@link Pages#request}, que es el unico sitio del proyecto donde se topa.
     */
    @Override
    public PageResult<Company> findAllVisibleTo(Long companyId, int page, int pageSize) {
        Pageable pageable = Pages.request(page, pageSize, BY_NAME_THEN_ID);
        Page<CompanyJpaEntity> result = companyId == null
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findPageByCompanyId(companyId, pageable);
        return Pages.result(result, mapper::toDomain);
    }

    /**
     * Mismo reparto de ramas que {@link #findAllVisibleTo}, con el termino añadido
     * al {@code WHERE}. El filtro de empresa se aplica <em>ademas</em> del termino,
     * nunca en su lugar: un empleado que busca solo puede encontrar su propia
     * empresa.
     */
    @Override
    public PageResult<Company> searchVisibleTo(Long companyId, String query, int page,
            int pageSize) {
        Pageable pageable = Pages.request(page, pageSize, BY_NAME_THEN_ID);
        Page<CompanyJpaEntity> result = companyId == null
                ? jpaRepository.searchByTerm(query, pageable)
                : jpaRepository.searchByCompanyAndTerm(companyId, query, pageable);
        return Pages.result(result, mapper::toDomain);
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
