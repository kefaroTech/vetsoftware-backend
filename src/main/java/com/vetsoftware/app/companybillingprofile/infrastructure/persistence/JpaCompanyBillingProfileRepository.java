package com.vetsoftware.app.companybillingprofile.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.companybillingprofile.application.port.out.CompanyBillingProfileRepository;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanyBillingProfileRepository implements CompanyBillingProfileRepository {

    private final CompanyBillingProfileJpaRepository jpaRepository;
    private final CityJpaRepository cityJpaRepository;
    private final CompanyBillingProfileJpaMapper mapper;

    public JpaCompanyBillingProfileRepository(CompanyBillingProfileJpaRepository jpaRepository,
            CityJpaRepository cityJpaRepository, CompanyBillingProfileJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.cityJpaRepository = cityJpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}: es el contrato del puerto, no
     * una preferencia.</strong>
     *
     * <p>
     * La sucesion cierra la ficha vigente y abre la siguiente en la misma
     * transaccion. Hibernate no manda esas dos sentencias en el orden en que se
     * llamo aqui: su cola de acciones ejecuta <em>todos</em> los {@code INSERT}
     * antes que los {@code UPDATE}. Con un {@code save} normal, la sucesora
     * entraria mientras la anterior sigue con {@code valid_to} nulo, las dos
     * calcularian el mismo {@code current_profile_marker} y
     * {@code uq_company_billing_profiles_current} pararia la operacion con un
     * {@code Duplicate entry} sobre una columna generada que nadie escribio.
     *
     * <p>
     * El caso de uso se lee perfecto —cierra primero, abre despues— y el defecto
     * estaria debajo, en el orden que decide el framework. Por eso el flush va
     * escrito en el puerto y se cumple aqui.
     *
     * <p>
     * <strong>{@code getReferenceById} y no {@code findById}</strong>: devuelve un
     * proxy sin {@code SELECT}, porque el municipio ya lo valido el caso de uso a
     * traves de {@code CityQueryPort}. Y el {@code toDomain} de vuelta reusa el
     * {@code CityRef} que traia la ficha, para no disparar la hidratacion de ese
     * mismo proxy.
     */
    @Override
    public CompanyBillingProfile save(CompanyBillingProfile profile) {
        CityJpaEntity city = cityJpaRepository.getReferenceById(profile.getCity().id());
        CompanyBillingProfileJpaEntity saved = jpaRepository
                .saveAndFlush(mapper.toJpa(profile, city));
        return mapper.toDomain(saved, profile.getCity());
    }

    @Override
    public Optional<CompanyBillingProfile> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<CompanyBillingProfile> findCurrentByCompanyId(Long companyId) {
        return jpaRepository.findCurrentByCompanyId(companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<CompanyBillingProfile> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, masReciente())), mapper::toDomain);
    }

    /**
     * Historico: la vigente primero y hacia atras, que es como se lee una linea de
     * tiempo. Desempate por {@code id} descendente para que el orden sea total —
     * {@code uq_company_billing_profiles_validity} impide dos fichas de la misma
     * empresa con la misma {@code valid_from}, asi que hoy el empate no puede
     * darse, pero un orden que depende de una restriccion de otra tabla es un orden
     * que se rompe el dia que esa restriccion cambie, y sin desempate dos paginas
     * consecutivas repiten u omiten filas.
     */
    private static Sort masReciente() {
        return Sort.by(Sort.Direction.DESC, "validFrom").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
