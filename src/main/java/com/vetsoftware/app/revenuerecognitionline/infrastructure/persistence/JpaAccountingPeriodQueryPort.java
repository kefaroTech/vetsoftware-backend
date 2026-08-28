package com.vetsoftware.app.revenuerecognitionline.infrastructure.persistence;

import com.vetsoftware.app.accountingperiod.infrastructure.persistence.AccountingPeriodJpaRepository;
import com.vetsoftware.app.revenuerecognitionline.application.port.out.AccountingPeriodQueryPort;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code accountingperiod}.
 *
 * <p>
 * <strong>Consulta {@code findOpenPeriodKeysFrom}, que proyecta la clave y no
 * la entidad, y eso es lo que mantiene el slicing intacto.</strong> El metodo
 * derivado equivalente —{@code findFirstByStatusAndPeriodKeyGreaterThanEqual}—
 * obligaria a pasar un {@code AccountingPeriodStatus}, es decir a importar el
 * <em>dominio</em> de otra feature, que es lo que el vertical slicing prohibe.
 * El literal del enum vive dentro del JPQL de la otra rodaja, que es donde si
 * puede estar.
 *
 * <p>
 * Pide <b>una sola fila</b>: el orden cronologico lo impone la propia consulta
 * y lo que hace falta aqui es el primer periodo abierto, no la lista.
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features.
 */
@Component("revenueRecognitionLineJpaAccountingPeriodQueryPort")
public class JpaAccountingPeriodQueryPort implements AccountingPeriodQueryPort {

    private final AccountingPeriodJpaRepository accountingPeriodJpaRepository;

    public JpaAccountingPeriodQueryPort(
            AccountingPeriodJpaRepository accountingPeriodJpaRepository) {
        this.accountingPeriodJpaRepository = accountingPeriodJpaRepository;
    }

    @Override
    public Optional<String> findFirstOpenPostingPeriodFrom(String periodKey) {
        if (periodKey == null)
            return Optional.empty();
        return accountingPeriodJpaRepository.findOpenPeriodKeysFrom(periodKey, Pages.request(0, 1))
                .stream().findFirst();
    }
}
