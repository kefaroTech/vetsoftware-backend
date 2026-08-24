package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListBillingDocumentsAwaitingExternalUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * La lista de trabajo pendiente de cada mes: lo calculado aquí y todavía sin
 * emitir fuera, de todas las clínicas.
 *
 * <p>
 * Sirve un listado que <b>no filtra por empresa</b>, así que su puerto está
 * cerrado a {@code hasRole("SYSTEM")} a secas
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). Es un barrido de plataforma y por
 * eso es legítimo; lo que no sería legítimo es servírselo a un empleado con el
 * permiso de lectura del tenant.
 */
@Observed(name = "subscription.billing.document.list.awaiting")
@Service
public class ListBillingDocumentsAwaitingExternalService
        implements
            ListBillingDocumentsAwaitingExternalUseCase {

    private final BillingDocumentRepository repository;

    public ListBillingDocumentsAwaitingExternalService(BillingDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<BillingDocumentDto> listAwaitingExternal(int page, int pageSize) {
        return repository.findAllAwaitingExternal(page, pageSize).map(BillingDocumentDto::from);
    }
}
