package com.vetsoftware.app.withholdingraterule.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaWithholdingRateRuleRepository implements WithholdingRateRuleRepository {

    private final WithholdingRateRuleJpaRepository jpaRepository;
    private final WithholdingRateRuleJpaMapper mapper;

    public JpaWithholdingRateRuleRepository(WithholdingRateRuleJpaRepository jpaRepository,
            WithholdingRateRuleJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public WithholdingRateRule save(WithholdingRateRule rule) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(rule)));
    }

    @Override
    public Optional<WithholdingRateRule> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<WithholdingRateRule> findAllEnabled(int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAllByEnabledTrue(Pages.request(page, pageSize, catalogOrder())),
                mapper::toDomain);
    }

    /**
     * <strong>Traduce el municipio nulo al centinela antes de preguntar.</strong>
     * Es la mitad Java del arreglo del changeset 317: la base guarda {@code '-'} en
     * la columna generada {@code municipality_key} para que dos tarifas nacionales
     * choquen en el indice unico, y la consulta tiene que comparar contra ese mismo
     * valor. Pasar el {@code null} tal cual devolveria cero filas para toda
     * retencion nacional, la retencion esperada saldria cero y no habria un solo
     * error que lo delatara.
     *
     * <p>
     * Pide <b>una sola fila</b> con el orden de {@link #effectiveOrder()}: si el
     * historico trae vigencias cerradas solapadas, se queda con la mas reciente en
     * vez de con «la primera que llegue».
     */
    @Override
    public Optional<WithholdingRateRule> findEffective(WithholdingType withholdingType,
            ServiceNature serviceNature, String municipalityCode, LocalDate on) {
        String municipalityKey = municipalityCode == null
                ? WithholdingRateRule.NATIONAL_MUNICIPALITY_KEY
                : municipalityCode;
        return jpaRepository
                .findEffective(withholdingType, serviceNature, municipalityKey, on,
                        Pages.request(0, 1, effectiveOrder()))
                .stream().findFirst().map(mapper::toDomain);
    }

    /**
     * Orden total del catalogo: agrupado por supuesto —tipo, naturaleza, municipio—
     * y dentro de cada uno lo mas reciente primero, con el {@code id} de desempate.
     *
     * <p>
     * El desempate no es adorno: dos reglas del mismo supuesto pueden compartir
     * {@code validFrom} si una esta cerrada y la otra no, y sin un criterio estable
     * dos paginas consecutivas pueden repetir u omitir filas.
     */
    private static Sort catalogOrder() {
        return Sort.by(Sort.Order.asc("withholdingType"), Sort.Order.asc("serviceNature"),
                Sort.Order.asc("municipalityCode"), Sort.Order.desc("validFrom"),
                Sort.Order.desc("id"));
    }

    /**
     * La mas reciente primero. Con el {@code id} de desempate porque dos filas
     * cerradas mal cargadas pueden compartir {@code validFrom}, y la respuesta a
     * «que tarifa aplico» no puede depender del plan que elija el motor.
     */
    private static Sort effectiveOrder() {
        return Sort.by(Sort.Order.desc("validFrom"), Sort.Order.desc("id"));
    }
}
