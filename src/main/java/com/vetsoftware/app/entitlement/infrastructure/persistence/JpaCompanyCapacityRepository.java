package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.UnreconciledCapacityQueryPort;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.LimitDimensionRef;
import com.vetsoftware.app.entitlement.domain.MeasureKind;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import com.vetsoftware.app.limitdimension.infrastructure.persistence.LimitDimensionJpaEntity;
import com.vetsoftware.app.limitdimension.infrastructure.persistence.LimitDimensionJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de salida de los contadores contratados.
 *
 * <p>
 * Conoce {@code limitdimension} por la excepcion acotada del {@code CLAUDE.md}:
 * la fila del contador copia el id del eje y su tipo de medida, pero no el
 * codigo ni la fecha en que el eje nacio, y las dos cosas hacen falta arriba
 * --el codigo para nombrar el eje, {@code available_from} para poder responder
 * a D-74--. El catalogo son ocho filas, asi que resolverlo por consulta directa
 * no tiene coste medible y evita meter una asociacion que arrastraria el grafo
 * entero.
 *
 * <p>
 * Implementa los <strong>dos</strong> puertos de esta tabla. El segundo
 * ({@link UnreconciledCapacityQueryPort}) es el unico que no se acota por
 * empresa, porque su consumidor es un barrido de plataforma; van juntos aqui
 * para que la unica clase que sabe leer y escribir {@code company_capacities}
 * siga siendo una.
 */
@Repository
public class JpaCompanyCapacityRepository
        implements
            CompanyCapacityRepository,
            UnreconciledCapacityQueryPort {

    private final CompanyCapacityJpaRepository jpaRepository;
    private final CompanyCapacityJpaMapper mapper;
    private final LimitDimensionJpaRepository limitDimensionJpaRepository;

    public JpaCompanyCapacityRepository(CompanyCapacityJpaRepository jpaRepository,
            CompanyCapacityJpaMapper mapper,
            LimitDimensionJpaRepository limitDimensionJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.limitDimensionJpaRepository = limitDimensionJpaRepository;
    }

    @Override
    public List<CompanyCapacity> findAllByCompanyId(Long companyId) {
        List<CompanyCapacityJpaEntity> rows = jpaRepository
                .findAllByCompany_IdOrderByLimitDimensionIdAscPeriodKeyAsc(companyId);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, DimensionIdentity> catalog = identitiesOf(rows.stream()
                .map(CompanyCapacityJpaEntity::getLimitDimensionId).collect(Collectors.toSet()));
        List<CompanyCapacity> result = new ArrayList<>(rows.size());
        for (CompanyCapacityJpaEntity row : rows) {
            DimensionIdentity identity = identityOf(catalog, row.getLimitDimensionId());
            result.add(mapper.toDomain(row, companyId, identity.code(), identity.availableFrom()));
        }
        return List.copyOf(result);
    }

    @Override
    public Optional<CompanyCapacity> findByCompanyIdAndDimension(Long companyId,
            Long limitDimensionId, String periodKey) {
        return jpaRepository.findByCompany_IdAndLimitDimensionIdAndPeriodKey(companyId,
                limitDimensionId, periodKey).map(entity -> {
                    DimensionIdentity identity = identityOf(identitiesOf(Set.of(limitDimensionId)),
                            limitDimensionId);
                    return mapper.toDomain(entity, companyId, identity.code(),
                            identity.availableFrom());
                });
    }

    @Override
    public int upsertCeilings(List<CompanyCapacity> capacities) {
        if (capacities.isEmpty()) {
            return 0;
        }
        int written = 0;
        for (CompanyCapacity capacity : capacities) {
            jpaRepository.upsertCeiling(capacity.getCompanyId(), capacity.getDimension().id(),
                    capacity.getDimension().measureKind().name(), capacity.getPeriodKey().value(),
                    capacity.getLimitQuantity(), capacity.getSubscriptionId(),
                    capacity.getLimitRecalculatedAt());
            written++;
        }
        return written;
    }

    @Override
    public int addUsage(Long companyId, Long limitDimensionId, String periodKey, int delta) {
        return jpaRepository.addUsage(companyId, limitDimensionId, periodKey, delta);
    }

    @Override
    public int addUsageAllowingOverage(Long companyId, Long limitDimensionId, String periodKey,
            int delta) {
        return jpaRepository.addUsageAllowingOverage(companyId, limitDimensionId, periodKey, delta);
    }

    @Override
    public int openPeriod(Long companyId, Long limitDimensionId, String periodKey,
            LocalDateTime at) {
        return jpaRepository.openPeriod(companyId, limitDimensionId, periodKey, at);
    }

    @Override
    public int markUsageReconciled(Long companyId, Long limitDimensionId, String periodKey,
            LocalDateTime at) {
        return jpaRepository.markUsageReconciled(companyId, limitDimensionId, periodKey, at);
    }

    @Override
    public List<CompanyCapacity> findUnreconciled(LocalDateTime staleBefore, long afterId,
            int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<UnreconciledCapacityView> rows = jpaRepository.findUnreconciled(staleBefore, afterId,
                limit);
        List<CompanyCapacity> result = new ArrayList<>(rows.size());
        for (UnreconciledCapacityView row : rows) {
            LimitDimensionRef dimension = new LimitDimensionRef(row.getLimitDimensionId(),
                    row.getDimensionCode(), MeasureKind.valueOf(row.getMeasureKind()),
                    row.getAvailableFrom());
            result.add(new CompanyCapacity(row.getId(), row.getCompanyId(), dimension,
                    PeriodKey.of(row.getPeriodKey()), row.getLimitQuantity(), row.getUsedQuantity(),
                    row.getSubscriptionId(), row.getLimitRecalculatedAt(),
                    row.getUsageReconciledAt(), row.getCreatedDate()));
        }
        return List.copyOf(result);
    }

    private Map<Long, DimensionIdentity> identitiesOf(Set<Long> dimensionIds) {
        Map<Long, DimensionIdentity> identities = new HashMap<>();
        for (LimitDimensionJpaEntity dimension : limitDimensionJpaRepository
                .findAllById(dimensionIds)) {
            identities.put(dimension.getId(),
                    new DimensionIdentity(dimension.getCode(), dimension.getAvailableFrom()));
        }
        return identities;
    }

    /**
     * Un contador que apunta a un eje que ya no se puede leer --desactivado, que es
     * lo unico posible porque la clave foranea va {@code RESTRICT}-- se denuncia en
     * vez de omitirse. Omitirlo lo haria indistinguible de "esta empresa no tiene
     * contador de ese eje", y eso se lee como techo cero: una funcion entera
     * bloqueada en silencio por haber desactivado una fila de catalogo.
     */
    private static DimensionIdentity identityOf(Map<Long, DimensionIdentity> identities,
            Long dimensionId) {
        DimensionIdentity identity = identities.get(dimensionId);
        if (identity == null) {
            throw new IllegalStateException("Capacity counter points to limit dimension "
                    + dimensionId + ", which is not readable (disabled?). Re-enable the row:"
                    + " dropping the counter would read as limit zero");
        }
        return identity;
    }

    /**
     * Lo que la fila del contador <em>no</em> copia y el dominio necesita: como se
     * llama el eje y desde cuando existe.
     */
    private record DimensionIdentity(String code, LocalDate availableFrom) {
    }
}
