package com.vetsoftware.app.companyactivitymonth.infrastructure.persistence;

import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyActivityMonthRepository;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonthAlreadyExistsException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanyActivityMonthRepository implements CompanyActivityMonthRepository {

    private final CompanyActivityMonthJpaRepository jpaRepository;
    private final CompanyActivityMonthJpaMapper mapper;

    public JpaCompanyActivityMonthRepository(CompanyActivityMonthJpaRepository jpaRepository,
            CompanyActivityMonthJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}, por dos motivos que apuntan
     * al mismo sitio.</strong>
     *
     * <p>
     * El primero es la traduccion del duplicado: {@code save} solo encola la
     * escritura, asi que la violacion de {@code uq_cam_month} saltaria al hacer
     * commit —fuera de este metodo y fuera de este {@code try}— y llegaria al
     * cliente como un error de integridad crudo en vez de como el 409 que dice que
     * el mes ya existe y que hay que recalcularlo. Con el flush explicito, el
     * choque ocurre donde se puede leer.
     *
     * <p>
     * El segundo es la version: {@code save} <b>no incrementa {@code @Version}
     * hasta el flush</b>, de modo que la entidad devuelta llevaria todavia la
     * version vieja. Hoy eso no se nota porque el DTO no la publica, pero el dia
     * que alguien encadene dos escrituras en la misma transaccion la segunda
     * llegaria con un numero caducado y el bloqueo optimista fallaria dando un 409
     * falso.
     */
    @Override
    public CompanyActivityMonth save(CompanyActivityMonth month) {
        try {
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(month)));
        } catch (DataIntegrityViolationException duplicado) {
            throw new CompanyActivityMonthAlreadyExistsException(month.getCompanyId(),
                    month.getPeriodKey().value(), duplicado);
        }
    }

    @Override
    public Optional<CompanyActivityMonth> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CompanyActivityMonth> findByCompanyIdAndPeriodKey(Long companyId,
            String periodKey) {
        return jpaRepository.findByCompanyIdAndPeriodKey(companyId, periodKey)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<CompanyActivityMonth> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, seriesOrder())),
                mapper::toDomain);
    }

    @Override
    public PageResult<CompanyActivityMonth> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, companySeriesOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<CompanyActivityMonth> findAllByPeriodKey(String periodKey, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByPeriodKey(periodKey,
                Pages.request(page, pageSize, periodOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<CompanyActivityMonth> findDormant(String periodKey, int activeDaysThreshold,
            int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAllByPeriodKeyAndActiveDaysLessThanEqual(periodKey,
                        activeDaysThreshold, Pages.request(page, pageSize, dormantOrder())),
                mapper::toDomain);
    }

    /**
     * El mes mas reciente primero y, dentro de el, las clinicas por identificador.
     *
     * <p>
     * <strong>El orden es total, con desempate por {@code id}</strong>, y eso no es
     * adorno: sin un criterio estable, dos paginas consecutivas del mismo listado
     * repiten u omiten filas segun el plan que elija el motor. Ordenar por
     * {@code periodKey} funciona como orden cronologico porque la columna es
     * {@code CHAR(7)} con colacion {@code ascii_bin} y {@code chk_cam_period_key}
     * garantiza la forma {@code AAAA-MM}.
     */
    private static Sort seriesOrder() {
        return Sort.by(Sort.Order.desc("periodKey"), Sort.Order.asc("companyId"),
                Sort.Order.desc("id"));
    }

    /** La serie de una clinica: lo mas reciente primero, con desempate por id. */
    private static Sort companySeriesOrder() {
        return Sort.by(Sort.Order.desc("periodKey"), Sort.Order.desc("id"));
    }

    /** La foto de un mes: por clinica, con desempate por id. */
    private static Sort periodOrder() {
        return Sort.by(Sort.Order.asc("companyId"), Sort.Order.desc("id"));
    }

    /**
     * Los mas dormidos primero: cero dias activos arriba del todo, que es el orden
     * en que alguien va a querer atacar la lista.
     */
    private static Sort dormantOrder() {
        return Sort.by(Sort.Order.asc("activeDays"), Sort.Order.asc("companyId"),
                Sort.Order.asc("id"));
    }
}
