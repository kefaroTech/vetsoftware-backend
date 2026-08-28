package com.vetsoftware.app.accountmapping.infrastructure.persistence;

import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAccountMappingRepository implements AccountMappingRepository {

    private final AccountMappingJpaRepository jpaRepository;
    private final AccountMappingJpaMapper mapper;

    public JpaAccountMappingRepository(AccountMappingJpaRepository jpaRepository,
            AccountMappingJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}.</strong> Sin el flush, la
     * violacion de {@code uq_account_mappings_current} —dos mapeos vigentes para el
     * mismo supuesto— saldria al cerrar la transaccion, fuera del caso de uso y sin
     * nadie que la traduzca. Con el, el duplicado llega donde se puede explicar.
     */
    @Override
    public AccountMapping save(AccountMapping mapping) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(mapping)));
    }

    @Override
    public Optional<AccountMapping> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<AccountMapping> findAllEnabled(int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAllByEnabledTrue(Pages.request(page, pageSize, catalogOrder())),
                mapper::toDomain);
    }

    /**
     * <strong>Traduce los tres afinados nulos a sus centinelas antes de
     * preguntar.</strong> Es la mitad Java del arreglo del changeset 343: la base
     * guarda {@code 0} y {@code '-'} en tres columnas generadas para que dos mapeos
     * iguales choquen en el indice unico, y la consulta tiene que comparar contra
     * esos mismos valores. Pasar los {@code null} tal cual devolveria cero filas
     * para nueve de las doce clases y el asiento no se generaria, sin un solo
     * error.
     */
    @Override
    public Optional<AccountMapping> findEffective(MappingKind mappingKind, String mappingKey,
            Long catalogItemId, String chargeType, String taxTreatment, LocalDate on) {
        Long catalogItemKey = catalogItemId == null
                ? AccountMapping.NO_CATALOG_ITEM_KEY
                : catalogItemId;
        String chargeTypeKey = chargeType == null ? AccountMapping.NO_REFINEMENT_KEY : chargeType;
        String taxTreatmentKey = taxTreatment == null
                ? AccountMapping.NO_REFINEMENT_KEY
                : taxTreatment;
        return jpaRepository
                .findEffective(mappingKind, mappingKey, catalogItemKey, chargeTypeKey,
                        taxTreatmentKey, on, Pages.request(0, 1, effectiveOrder()))
                .stream().findFirst().map(mapper::toDomain);
    }

    /**
     * Orden total del catalogo: agrupado por supuesto y dentro de cada uno lo mas
     * reciente primero, con el {@code id} de desempate. Sin un criterio estable dos
     * paginas consecutivas pueden repetir u omitir filas.
     */
    private static Sort catalogOrder() {
        return Sort.by(Sort.Order.asc("mappingKind"), Sort.Order.asc("mappingKey"),
                Sort.Order.desc("validFrom"), Sort.Order.desc("id"));
    }

    /**
     * El mas reciente primero. Con el {@code id} de desempate porque dos filas
     * cerradas mal cargadas pueden compartir {@code validFrom}, y la respuesta a
     * «contra que cuenta se asento» no puede depender del plan que elija el motor.
     */
    private static Sort effectiveOrder() {
        return Sort.by(Sort.Order.desc("validFrom"), Sort.Order.desc("id"));
    }
}
