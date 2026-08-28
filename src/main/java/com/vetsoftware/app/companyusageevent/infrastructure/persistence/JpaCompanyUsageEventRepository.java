package com.vetsoftware.app.companyusageevent.infrastructure.persistence;

import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanyUsageEventRepository implements CompanyUsageEventRepository {

    private final CompanyUsageEventJpaRepository jpaRepository;
    private final CompanyUsageEventJpaMapper mapper;

    public JpaCompanyUsageEventRepository(CompanyUsageEventJpaRepository jpaRepository,
            CompanyUsageEventJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CompanyUsageEvent save(CompanyUsageEvent event) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(event)));
    }

    @Override
    public Optional<CompanyUsageEvent> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CompanyUsageEvent> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<CompanyUsageEvent> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, timelineOrder())),
                mapper::toDomain);
    }

    @Override
    public PageResult<CompanyUsageEvent> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, timelineOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<CompanyUsageEvent> findAllByCompanyIdAndChargeId(Long companyId,
            Long chargeId, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyIdAndChargeId(companyId, chargeId,
                Pages.request(page, pageSize, timelineOrder())), mapper::toDomain);
    }

    /**
     * Lo mas reciente primero, con el {@code id} de desempate.
     *
     * <p>
     * <strong>El desempate no es adorno.</strong> Dos hechos del mismo eje pueden
     * compartir {@code occurred_at} al microsegundo —dos mascotas dadas de alta en
     * la misma importacion masiva lo hacen a diario—, y sin un criterio estable dos
     * paginas consecutivas repiten u omiten filas. Sobre la tabla que sostiene un
     * cobro, «se salto un hecho al paginar» es un excedente que no se factura.
     *
     * <p>
     * El orden es descendente porque las dos consultas que importan —el expediente
     * de una reclamacion y el barrido del cierre— miran siempre lo ultimo.
     */
    private static Sort timelineOrder() {
        return Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));
    }
}
