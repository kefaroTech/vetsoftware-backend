package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionpayment.application.port.out.WithholdingQueryPort;
import com.vetsoftware.app.subscriptionpayment.domain.WithholdingRef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Lee la retencion del slice {@code documentwithholding} <b>siempre acotada por
 * empresa</b>.
 *
 * <p>
 * <b>Consulta nativa y solo cuatro escalares</b>, mismo criterio que
 * {@link JpaBillingDocumentQueryPort}: el detalle fiscal —tipo, base, tarifa,
 * municipio, periodo gravable, certificado— <b>no se trae</b>. Copiarlo aqui
 * crearia una segunda version de cifras que se declaran ante la DIAN, y dos
 * versiones de eso es exactamente el descuadre que aparece en la declaracion.
 *
 * <p>
 * Es una lectura: no la toca {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}.
 */
@Component
public class JpaWithholdingQueryPort implements WithholdingQueryPort {

    private static final String SELECT_ACOTADO = """
            SELECT w.id, w.company_id, w.billing_document_id, w.amount
            FROM document_withholdings w
            WHERE w.id = :withholdingId
              AND w.company_id = :companyId
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<WithholdingRef> findByIdAndCompanyId(Long withholdingId, Long companyId) {
        if (withholdingId == null || companyId == null)
            return Optional.empty();
        List<?> filas = entityManager.createNativeQuery(SELECT_ACOTADO)
                .setParameter("withholdingId", withholdingId).setParameter("companyId", companyId)
                .setMaxResults(1).getResultList();
        if (filas.isEmpty())
            return Optional.empty();
        Object[] fila = (Object[]) filas.get(0);
        return Optional.of(
                new WithholdingRef(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue(),
                        ((Number) fila[2]).longValue(), (BigDecimal) fila[3]));
    }
}
