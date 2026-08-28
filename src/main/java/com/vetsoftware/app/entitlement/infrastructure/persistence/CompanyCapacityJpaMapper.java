package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.LimitDimensionRef;
import com.vetsoftware.app.entitlement.domain.MeasureKind;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el contador de dominio y su fila.
 *
 * <p>
 * <strong>Solo traduce de fila a dominio.</strong> No hay {@code toJpa}: el
 * techo se escribe con la sentencia de {@code upsertCeiling}, que no nombra la
 * columna del consumo (#648), y ofrecer aqui una conversion de vuelta seria
 * ofrecer el camino que hay que evitar --guardar la fila entera con el consumo
 * que se leyo hace un instante--.
 *
 * <p>
 * El {@code code} del eje y su {@code available_from} no viven en esta tabla:
 * la fila copia el id y el tipo de medida, que es lo que la clave foranea
 * compuesta ata. Los pone quien lee, y por eso {@link #toDomain} los recibe.
 *
 * <p>
 * <strong>El {@code measureKind} se toma de la fila y no del catalogo</strong>,
 * aunque el que lee traiga las dos cosas: la copia de la fila es la que espeja
 * {@code chk_company_capacities_period_key} y la que la clave foranea compuesta
 * ata (R-LIMIT-22). Tomarlo del catalogo haria que una fila incoherente se
 * leyera coherente, que es justo lo que esa atadura existe para impedir.
 */
@Component
public class CompanyCapacityJpaMapper {

    public CompanyCapacity toDomain(CompanyCapacityJpaEntity entity, Long companyId,
            String dimensionCode, LocalDate dimensionAvailableFrom) {
        LimitDimensionRef dimension = new LimitDimensionRef(entity.getLimitDimensionId(),
                dimensionCode, MeasureKind.valueOf(entity.getMeasureKind()),
                dimensionAvailableFrom);
        return new CompanyCapacity(entity.getId(), companyId, dimension,
                PeriodKey.of(entity.getPeriodKey()), entity.getLimitQuantity(),
                entity.getUsedQuantity(), entity.getSubscriptionId(),
                entity.getLimitRecalculatedAt(), entity.getUsageReconciledAt(),
                entity.getCreatedDate());
    }
}
