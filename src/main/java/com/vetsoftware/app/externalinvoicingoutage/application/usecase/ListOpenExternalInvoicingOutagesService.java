package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.ListOpenExternalInvoicingOutagesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Que esta caido ahora mismo.
 *
 * <p>
 * La lista no se pagina porque no puede crecer: {@code uq_eio_open} admite una
 * sola caida abierta por causante y los causantes son cuatro. Ese tope lo pone
 * el esquema, no este servicio, y por eso no hace falta defenderlo aqui.
 */
@Observed(name = "external.invoicing.outage.list.open")
@Service
public class ListOpenExternalInvoicingOutagesService
        implements
            ListOpenExternalInvoicingOutagesUseCase {

    private final ExternalInvoicingOutageRepository repository;

    public ListOpenExternalInvoicingOutagesService(ExternalInvoicingOutageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalInvoicingOutageDto> listOpen() {
        return repository.findAllOpen().stream().map(ExternalInvoicingOutageDto::from).toList();
    }
}
