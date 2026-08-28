package com.vetsoftware.app.accountingperiod.application.usecase;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.in.ResolvePostingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodKey;
import com.vetsoftware.app.accountingperiod.domain.NoOpenAccountingPeriodException;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * En que mes se registra un hecho ocurrido en una fecha dada.
 *
 * <p>
 * <strong>Nunca hacia atras.</strong> La fecha del hecho dice donde <em>empezar
 * a buscar</em>, no donde registrar: si su mes esta abierto se imputa ahi, y si
 * no, en el primer mes abierto posterior. Imputar hacia atras —al ultimo mes
 * que todavia admita escrituras— es lo intuitivo y es exactamente lo que rompe
 * la ficha: el informe de marzo tiene que seguir dando lo que se declaro en
 * marzo. Un ajuste que aparece en un mes ya cerrado cambia un numero que ya
 * salio de la empresa, y no hay error que lo delate — solo dos documentos que
 * dejan de coincidir.
 *
 * <p>
 * <strong>Sin {@code @Transactional} y sin reloj</strong>: es una lectura de
 * una sola consulta, y la fecha la trae el hecho, no el sistema. Inyectar aqui
 * un {@code Clock} sugeriria que «hoy» participa en la decision, y no
 * participa.
 *
 * <p>
 * <strong>Las dos ramas caben en una consulta</strong>,
 * {@code findFirstOpenFrom}, porque el orden lexicografico de {@code yyyy-MM}
 * es el cronologico. Partirlas en dos —«busca el exacto; si no esta abierto,
 * busca el siguiente»— costaria dos viajes a la base y una ventana entre ellos
 * en la que el mes exacto se cierra.
 */
@Observed(name = "accounting.period.resolve.posting")
@Service
public class ResolvePostingPeriodService implements ResolvePostingPeriodUseCase {

    private final AccountingPeriodRepository repository;

    public ResolvePostingPeriodService(AccountingPeriodRepository repository) {
        this.repository = repository;
    }

    @Override
    public AccountingPeriodDto resolve(LocalDate occurredOn) {
        AccountingPeriodKey desde = AccountingPeriodKey.from(occurredOn);
        return repository.findFirstOpenFrom(desde).map(AccountingPeriodDto::from)
                .orElseThrow(() -> new NoOpenAccountingPeriodException(desde));
    }
}
