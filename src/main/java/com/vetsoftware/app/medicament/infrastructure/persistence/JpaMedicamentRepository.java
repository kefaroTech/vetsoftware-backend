package com.vetsoftware.app.medicament.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMedicamentRepository implements MedicamentRepository {

    /**
     * Orden por nombre, que es como se lee un catalogo, con el id de desempate para
     * que la paginacion sea determinista: sin el, dos homonimos pueden cambiar de
     * pagina entre dos peticiones y una fila se repite mientras otra no aparece
     * nunca.
     */
    private static final Sort PAGE_ORDER = Sort.by(Sort.Direction.ASC, "name")
            .and(Sort.by(Sort.Direction.ASC, "id"));

    private final MedicamentJpaRepository jpaRepository;
    private final MedicamentJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaMedicamentRepository(MedicamentJpaRepository jpaRepository,
            MedicamentJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    /**
     * El termino de busqueda, o {@code null} si no hay ninguno. En blanco equivale
     * a ausente: {@code null} viaja a la consulta como «sin filtro» y el listado se
     * comporta como antes de existir la busqueda, de modo que un campo de texto
     * vacio en el front no cambia nada.
     *
     * <p>
     * Los comodines NO se ponen aqui: los pone la consulta con
     * {@code LIKE LOWER(CONCAT('%', :q, '%'))}, que es el precedente de
     * {@code CompanyJpaRepository.searchByTerm} y
     * {@code OwnerJpaRepository.searchByCompanyAndTerm}. Es SUBCADENA y no prefijo,
     * y aqui la subcadena hace falta de verdad: media nomenclatura farmacologica es
     * compuesta, asi que «clavulanico» tiene que encontrar «Amoxicilina + Acido
     * clavulanico», invisible para un prefijo.
     *
     * <p>
     * Lo unico que se toca es el recorte. Ni la caja ni los acentos se normalizan
     * en Java a proposito: eso lo resuelve la base con la collation de la columna,
     * que es el MISMO criterio con el que el indice unico decide si un nombre esta
     * ocupado. Normalizar aqui es justamente como se consigue que buscar y chocar
     * dejen de responder a lo mismo.
     */
    private static String termino(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }

    @Override
    public Medicament save(Medicament medicament) {
        CompanyJpaEntity company = medicament.getCompany() == null
                ? null
                : companyJpaRepository.getReferenceById(medicament.getCompany().id());
        MedicamentJpaEntity saved = jpaRepository.save(mapper.toJpa(medicament, company));
        return mapper.toDomain(saved, medicament.getCompany());
    }

    @Override
    public Optional<Medicament> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Medicament> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<Medicament> findAvailableByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findAvailableById(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<Medicament> findAll(String q, int page, int pageSize) {
        Page<MedicamentJpaEntity> result = jpaRepository.search(termino(q),
                Pages.request(page, pageSize, PAGE_ORDER));
        return Pages.result(result, mapper::toDomain);
    }

    @Override
    public PageResult<Medicament> findAllGlobal(String q, int page, int pageSize) {
        Page<MedicamentJpaEntity> result = jpaRepository.searchGlobal(termino(q),
                Pages.request(page, pageSize, PAGE_ORDER));
        return Pages.result(result, mapper::toDomain);
    }

    @Override
    public List<Medicament> findAllAvailableForCompany(Long companyId) {
        return jpaRepository.findAllByGeneralTrueOrCompany_Id(companyId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Medicament> findAllDisabledForCompany(Long companyId) {
        return jpaRepository.findAllDisabledForCompany(companyId).stream().map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Medicament> findAllDisabledGlobal() {
        return jpaRepository.findAllDisabledGlobal().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }

    @Override
    public int reactivateGlobal(Long id) {
        return jpaRepository.reactivateGlobal(id);
    }

    @Override
    public Optional<Medicament> findByNameAndCompanyIdIncludingDisabled(String name,
            Long companyId) {
        return (companyId == null
                ? jpaRepository.findGlobalByNameIncludingDisabled(name)
                : jpaRepository.findByNameAndCompanyIncludingDisabled(name, companyId))
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveByNameAndCompanyIdExcludingId(String name, Long companyId, Long id) {
        return companyId == null
                ? jpaRepository.existsByNameAndCompanyIsNullAndIdNot(name, id)
                : jpaRepository.existsByNameAndCompany_IdAndIdNot(name, companyId, id);
    }

    @Override
    public int reactivateWithDetails(Long id, Long companyId, String name, String description) {
        return companyId == null
                ? jpaRepository.reactivateWithDetails(id, name, description)
                : jpaRepository.reactivateWithDetails(id, companyId, name, description);
    }
}
