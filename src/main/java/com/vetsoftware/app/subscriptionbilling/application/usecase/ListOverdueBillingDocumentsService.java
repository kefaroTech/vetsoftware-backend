package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListOverdueBillingDocumentsUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * El barrido de mora de la plataforma.
 *
 * <p>
 * El «hoy» sale del {@link Clock} inyectado y entra en la consulta como
 * parámetro. No podría estar dentro del marcador: las expresiones de columna
 * generada tienen que ser deterministas y {@code CURRENT_DATE} no lo es. Que
 * llegue por el reloj inyectado, y no de un {@code LocalDate.now()} suelto, es
 * lo que permite fijar en un test el caso que solo aparece al cambiar el día.
 */
@Observed(name = "subscription.billing.document.list.overdue")
@Service
public class ListOverdueBillingDocumentsService implements ListOverdueBillingDocumentsUseCase {

    private final BillingDocumentRepository repository;
    private final Clock clock;

    public ListOverdueBillingDocumentsService(BillingDocumentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public PageResult<BillingDocumentDto> listOverdue(int page, int pageSize) {
        return repository.findAllOverdue(LocalDate.now(clock), page, pageSize)
                .map(BillingDocumentDto::from);
    }
}
