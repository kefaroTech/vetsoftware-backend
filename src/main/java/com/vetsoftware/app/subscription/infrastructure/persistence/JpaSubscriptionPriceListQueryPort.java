package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.subscription.domain.PriceListRef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Lee la tarifa de la cabecera del contrato con su estado y su ventana.
 *
 * <p>
 * Nativa y no derivada del nombre por lo mismo que su gemela de la cotizacion:
 * hay que ver {@code status} y las dos fechas en la misma fila, y el
 * {@code @SQLRestriction} de la entidad no basta para expresar «publicada». No
 * decide vigencia —eso es del caso de uso, que tiene el reloj zonado—: solo
 * trae los datos con los que se decide.
 */
@Component("subscriptionJpaPriceListQueryPort")
public class JpaSubscriptionPriceListQueryPort implements PriceListQueryPort {

    private final EntityManager entityManager;

    public JpaSubscriptionPriceListQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<PriceListRef> findPublishedById(Long priceListId) {
        if (priceListId == null)
            return Optional.empty();
        Query query = entityManager.createNativeQuery("""
                SELECT id, code, valid_from, valid_to
                  FROM price_lists
                 WHERE id = :id
                   AND status = 'PUBLISHED'
                   AND enabled = TRUE
                """).setParameter("id", priceListId);
        List<?> rows = query.getResultList();
        if (rows.isEmpty())
            return Optional.empty();
        Object[] row = (Object[]) rows.get(0);
        return Optional.of(new PriceListRef(((Number) row[0]).longValue(), row[1].toString(),
                date(row[2]), date(row[3])));
    }

    private static LocalDate date(Object value) {
        return switch (value) {
            case null -> null;
            case LocalDate localDate -> localDate;
            case Date sqlDate -> sqlDate.toLocalDate();
            default -> LocalDate.parse(value.toString());
        };
    }
}
